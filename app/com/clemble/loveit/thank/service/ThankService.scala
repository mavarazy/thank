package com.clemble.loveit.thank.service

import com.clemble.loveit.common.model.{Resource, UserID}
import com.clemble.loveit.payment.service.ThankTransactionService
import com.clemble.loveit.thank.model.Thank
import com.clemble.loveit.thank.service.repository.ThankRepository
import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}

trait ThankService {

  def getOrCreate(uri: Resource): Future[Thank]

  def thank(giver: UserID, uri: Resource): Future[Thank]

}

@Singleton
case class SimpleThankService @Inject()(
                                         transactionService: ThankTransactionService,
                                         repo: ThankRepository,
                                         implicit val ec: ExecutionContext
) extends ThankService {

  override def getOrCreate(resource: Resource): Future[Thank] = {
    def createIfMissing(thankOpt: Option[Thank]): Future[Thank] = {
      thankOpt match {
        case Some(thank) => Future.successful(thank)
        case None =>
          resource.parent() match {
            case Some(parRes) =>
              for {
                owner <- getOrCreate(parRes).map(_.owner)
                thank = Thank(resource, owner)
                createdNew <- repo.save(thank)
                created <- if(createdNew) Future.successful(thank) else repo.findByResource(resource).map(_.get)
              } yield {
                created
              }
            case None => // TODO define proper error handling here
              throw new IllegalArgumentException()
          }
      }
    }

    repo.findByResource(resource).flatMap(createIfMissing)
  }

  override def thank(giver: UserID, resource: Resource): Future[Thank] = {
    for {
      owner <- getOrCreate(resource).map(_.owner) // Ensure Thank exists
      increased <- repo.increase(giver, resource) if (increased)
      _ <- transactionService.create(giver, owner, resource)
      updated <- repo.findByResource(resource).map(_.get)
    } yield {
      updated
    }
  }

}