package com.clemble.loveit.dev.service

import java.time.LocalDate
import javax.inject.Inject

import akka.actor.{Actor, Props}
import com.clemble.loveit.common.model.{HttpResource, Resource, UserID}
import com.clemble.loveit.common.util.{AuthEnv, EventBusManager, IDGenerator}
import com.clemble.loveit.thank.service.{ROService, PostService}
import com.clemble.loveit.user.model.User
import com.clemble.loveit.user.service.UserService
import com.mohiva.play.silhouette.api._

import scala.concurrent.{ExecutionContext, Future}

case class DevSignUpListener(resources: Seq[Resource], thankService: PostService) extends Actor {

  implicit val ec = context.dispatcher

  def runThanks(giver: UserID) = {
    for {
      res <- resources
    } yield {
      thankService.thank(giver, res)
    }
  }

  override def receive: Receive = {
    case SignUpEvent(user: User, _) =>
      runThanks(user.id)
    case LoginEvent(user: User, _) =>
      runThanks(user.id)
  }
}

trait DevUserDataService {

  def enable(resMap: Map[User, Resource]): Future[Boolean]

}

/**
  * Service that creates first users and integrations for testing UI and UX
  */
case class SimpleDevUserDataService @Inject()(
                                               userService: UserService,
                                               roService: ROService,
                                               thankService: PostService,
                                               eventBusManager: EventBusManager,
                                               implicit val ec: ExecutionContext
                                             ) extends DevUserDataService {

  // quotes, inspirational, motivational, cartoons, comics, webcomic, inspire, inspiring, art, poetry

  val resMap = Map(
    User(
      id = IDGenerator.generate(),
      firstName = Some("Gavin"),
      lastName = Some("Than"),
      email = "gavin.than@example.com",
      profiles = Set(LoginInfo("patreon", "zenpencil")),
      avatar = Some("https://pbs.twimg.com/profile_images/493961823763181568/mb_2vK6y_400x400.jpeg"),
      link = Some("https://zenpencils.com")
    ) -> HttpResource("zenpencils.com"),
    User(
      id = IDGenerator.generate(),
      firstName = Some("Manga"),
      lastName = Some("Stream"),
      email = "manga.stream@example.com",
      profiles = Set(LoginInfo("patreon", "mangastream")),
      avatar = Some("https://pbs.twimg.com/profile_images/544145066/twitterpic_400x400.png"),
      link = Some("https://readms.net")
    ) -> HttpResource("readms.net"),
    User(
      id = IDGenerator.generate(),
      firstName = Some("Personal"),
      lastName = Some("Central"),
      avatar = Some("https://pbs.twimg.com/profile_images/741421578370572288/l1pjJGbp_400x400.jpg"),
      email = "personal.central@example.com",
      profiles = Set(LoginInfo("patreon", "personal.central")),
      link = Some("https://personacentral.com")
    ) -> HttpResource("personacentral.com")
  )

  val RESOURCES_TO_LOVE = List(
    Resource.from("https://zenpencils.com/comic/poison/"),
    Resource.from("https://zenpencils.com/comic/hustle/"),
    Resource.from("https://zenpencils.com/comic/poe/"),
  )

  enable(resMap)


  override def enable(resMap: Map[User, Resource]): Future[Boolean] = {
    (for {
      creatorsToRes <- ensureCreators(resMap)
      resources <- assignOwnership(creatorsToRes)
    } yield {
      val allResources = resources ++ RESOURCES_TO_LOVE
      eventBusManager.onSignUp(Props(DevSignUpListener(allResources, thankService)))
      eventBusManager.onLogin(Props(DevSignUpListener(allResources, thankService)))
    }).recover({
      case t: Throwable => {
        print(t)
        System.exit(1)
        false
      }
    })
  }

  private def ensureCreators(resMap: Map[User, Resource]): Future[Map[User, Resource]] = {
    val fCreators = for {
      (creator, resources) <- resMap
    } yield {
      userService.
        findByEmail(creator.email).
        flatMap({
          case Some(user) => Future.successful(user -> resources)
          case None => {
            userService.create(creator).map(creator => {
              eventBusManager.publish(SignUpEvent(creator, null))
              creator -> resources
            })
          }
        })
    }
    Future.sequence(fCreators).map(_.toMap)
  }

  private def assignOwnership(creatorToRes: Map[User, Resource]): Future[List[Resource]] = {
    val resources = for {
      (creator, resource) <- creatorToRes
    } yield {
      roService.
        assignOwnership(creator.id, resource).
        recoverWith({
          case _: Throwable =>
            Thread.sleep(10000)
            roService.assignOwnership(creator.id, resource)
          }
        )
    }
    Future.sequence(resources).map(_.toList)
  }

}
