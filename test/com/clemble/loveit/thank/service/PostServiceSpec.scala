package com.clemble.loveit.thank.service

import com.clemble.loveit.common.error.ResourceException
import com.clemble.loveit.common.model.{Amount, HttpResource, Resource, UserID}
import com.clemble.loveit.payment.service.PaymentServiceTestExecutor
import com.clemble.loveit.thank.model.SupportedProject
import com.clemble.loveit.thank.service.repository.PostRepository
import org.junit.runner.RunWith
import org.specs2.concurrent.ExecutionEnv
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class PostServiceSpec(implicit val ee: ExecutionEnv) extends PaymentServiceTestExecutor {

  val thankService = dependency[PostService]
  val thankRepo = dependency[PostRepository]
  val supportedProjectService = dependency[SupportedProjectService]

  def createScene():(Resource, UserID, UserID) = {
    val url = HttpResource(s"example.com/some/${someRandom[Long]}")
    // TODO flow must be changed here to use ResourceOwnersip verification
    val owner = createUser()
    val project = SupportedProject from getUser(owner).get
    await(roService.assignOwnership(owner, url))
    await(thankRepo.updateOwner(project, url))
    val giver = createUser()

    (url, owner, giver)
  }

  def thank(user: UserID, url: Resource) = {
    await(thankService.thank(user, url))
  }

  def getBalance(url: Resource): Amount = {
    await(thankService.getOrCreate(url)).thank.given
  }

  "thanked" should {

    "return false on random res" in {
      val user = someRandom[UserID]
      val res = someRandom[Resource]

      await(thankService.hasSupported(user, res)) should throwA[ResourceException]
    }

    "return false on not thanked res" in {
      val (res, _, giver) = createScene()

      await(thankService.hasSupported(giver, res)) shouldEqual false
    }

    "return true if thanked" in {
      val (res, _, giver) = createScene()

      await(thankService.thank(giver, res))
      await(thankService.hasSupported(giver, res)) shouldEqual true
    }

  }


  "Thank " should {

    "Decrement for the giver" in {
      val (url, _, giver) = createScene()

      thank(giver, url)
      eventually(getBalance(url) shouldEqual 1)

      eventually(getBalance(giver) shouldEqual -1)
    }

    "Increment for the owner" in {
      val (url, owner, giver) = createScene()

      thank(giver, url)
      eventually(getBalance(url) shouldEqual 1)

      eventually(getBalance(owner) shouldEqual 1)
    }

    "Double thank has no effect" in {
      val (url, owner, giver) = createScene()

      getBalance(owner) shouldEqual 0
      getBalance(giver) shouldEqual 0

      // Double thank has no effect
      thank(giver, url)
      thank(giver, url)
      thank(giver, url)
      eventually(getBalance(url) shouldEqual 1)

      // Balance did not change
      eventually(getBalance(owner) shouldEqual 1)
      eventually(getBalance(giver) shouldEqual - 1)
    }

  }

}