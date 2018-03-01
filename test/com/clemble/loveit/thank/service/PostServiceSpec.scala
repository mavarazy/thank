package com.clemble.loveit.thank.service

import com.clemble.loveit.common.error.ResourceException
import com.clemble.loveit.common.model.{Amount, Resource, UserID}
import com.clemble.loveit.payment.service.PaymentServiceTestExecutor
import com.clemble.loveit.thank.model.{OpenGraphObject, Project}
import com.clemble.loveit.thank.service.repository.PostRepository
import org.junit.runner.RunWith
import org.specs2.concurrent.ExecutionEnv
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class PostServiceSpec(implicit val ee: ExecutionEnv) extends PaymentServiceTestExecutor {

  val service = dependency[PostService]
  val repo = dependency[PostRepository]
  val supportedProjectService = dependency[ProjectService]

  def createScene():(Resource, UserID, UserID) = {
    val owner = createUser()
    val giver = createUser()

    val url = s"https://example.com/some/${someRandom[Long]}"

    createProject(owner, url)
    await(service.create(someRandom[OpenGraphObject].copy(url = url)))

    (url, owner, giver)
  }

  def thank(user: UserID, url: Resource) = {
    await(service.thank(user, url))
  }

  def getBalance(url: Resource): Amount = {
    await(service.getPostOrProject(url)) match {
      case Left(post) => post.thank.given
      case _ => 0
    }
  }

  "thanked" should {

    "return throw Exception on random res" in {
      val user = someRandom[UserID]
      val res = randomResource

      await(service.hasSupported(user, res)) should throwA[ResourceException]
      await(service.hasSupported(user, res)) should throwA[ResourceException]
    }

    "return false on not thanked res" in {
      val (res, _, giver) = createScene()

      await(service.hasSupported(giver, res)) shouldEqual false
    }

    "return true if thanked" in {
      val (res, _, giver) = createScene()

      await(service.thank(giver, res))
      await(service.hasSupported(giver, res)) shouldEqual true
    }

  }

  "UPDATE OWNER" should {

    "create if missing" in {
      val owner = createUser()
      val url = randomResource

      await(service.getPostOrProject(url)) should throwA()

      createProject(owner, url).url shouldEqual url
      await(service.getPostOrProject(url)).right.exists(_.user == owner) should beTrue
    }

    "update if exists" in {
      val url = randomResource

      val A = createUser()

      createProject(A, url).url shouldEqual url
      await(service.getPostOrProject(url)).isRight shouldEqual true
      await(service.getPostOrProject(url)).right.exists(_.user == A) should beTrue

      val B = createUser()

      createProject(B, url).url shouldEqual url
      await(service.getPostOrProject(url)).isRight shouldEqual true
      await(service.getPostOrProject(url)).right.exists(_.user == B) should beTrue
    }

  }

}
