package com.clemble.loveit.user.service

import com.clemble.loveit.common.model.{Amount, UserID}
import com.clemble.loveit.user.model._
import com.clemble.loveit.payment.model.BankDetails
import com.clemble.loveit.user.service.repository.UserRepository
import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}

@Singleton
case class SimpleUserService @Inject()(repository: UserRepository, implicit val ec: ExecutionContext) extends UserService {

  override def findById(id: UserID): Future[Option[User]] = {
    repository.findById(id)
  }

}
