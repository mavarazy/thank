package com.clemble.loveit.thank.service


import com.clemble.loveit.common.error.UserException
import com.clemble.loveit.common.model.{Amount, Resource, UserID}
import com.clemble.loveit.thank.model.{ResourceOwnership, Thank}
import com.clemble.loveit.user.model.User
import com.clemble.loveit.user.service.repository.UserRepository
import javax.inject.{Inject, Singleton}

import com.clemble.loveit.thank.service.repository.ThankRepository

import scala.concurrent.{ExecutionContext, Future}

trait ResourceOwnershipService {

  def list(user: UserID): Future[Set[ResourceOwnership]]

  def findResourceOwner(uri: Resource): Future[User]

  def assign(user: UserID, ownership: ResourceOwnership):  Future[ResourceOwnership]

  def updateBalance(user: UserID, change: Amount): Future[Boolean]

}

@Singleton
case class SimpleResourceOwnershipService @Inject() (userRepo: UserRepository, thankRepo: ThankRepository, implicit val ec: ExecutionContext) extends ResourceOwnershipService {

  override def list(user: UserID): Future[Set[ResourceOwnership]] = {
    userRepo.findById(user).map(_.map(_.owns).getOrElse(Set.empty))
  }

  override def assign(userId: UserID, ownership: ResourceOwnership): Future[ResourceOwnership] = {
    def canOwn(users: List[User]): Boolean = {
      val allOwned = users.flatMap(_.owns)
      val oneOfAlreadyOwned = allOwned.contains(ownership)
      val alreadyFullyOwned = allOwned.exists(_.owns(ownership.resource))
      !(oneOfAlreadyOwned || alreadyFullyOwned)
    }

    def toPendingBalance(relatedUsers: List[User]): Future[Amount] = {
      val realizedUsers = relatedUsers.filter(_.owns.map(_.resource).exists(ownership.owns))
      for {
        _ <- userRepo.remove(relatedUsers.map(_.id))
      } yield {
        realizedUsers.map(_.balance).sum
      }
    }

    for {
      ownerOpt <- chooseOwner(ownership.resource)
      relatedUsers <- userRepo.findRelated(ownership)
      pendingBalance <- toPendingBalance(relatedUsers)
      userOpt <- userRepo.findById(userId)
    } yield {
      if (ownerOpt.isDefined)
      throw UserException.resourceAlreadyOwned(ownerOpt.get)
      if (userOpt.isEmpty)
      throw UserException.userMissing(userId)
      if (relatedUsers.exists(_ == userOpt.get))
      throw UserException.userMissing(userId)
      if (!canOwn(relatedUsers))
      throw UserException.resourceOwnershipImpossible()
      thankRepo.save(Thank(ownership.resource, userOpt.get.id))
      userRepo.update(userOpt.get.assignOwnership(pendingBalance, ownership))
      ownership
    }
  }

  private def chooseOwner(uri: Resource): Future[Option[User]] = {
    val ownerships = ResourceOwnership.toPossibleOwnerships(uri)
    for {
      owners <- userRepo.findOwners(ownerships)
    } yield {
      val resToOwner = owners.
        flatMap(owner => {
          owner.owns.filter(ownerships.contains).map(res => res.resource -> owner)
        }).
        sortBy({ case (resource, _) => - resource.uri.length })

      resToOwner.headOption.map({ case (_, owner) => owner })
    }
  }

  override def findResourceOwner(uri: Resource): Future[User] = {
    def createOwnerIfMissing(userOpt: Option[User]): Future[User] = {
      userOpt match {
        case Some(id) => Future.successful(id)
        case None => userRepo.save(User.empty(uri))
      }
    }

    for {
      ownerOpt <- chooseOwner(uri)
      owner <- createOwnerIfMissing(ownerOpt)
    } yield {
      owner
    }
  }

  // TODO this is duplication need to be unified and used only once
  override def updateBalance(user: UserID, change: Amount): Future[Boolean] = {
    userRepo.changeBalance(user, change)
  }

}
