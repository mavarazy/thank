package com.clemble.loveit.thank.service

import com.clemble.loveit.common.model.{HttpResource, Resource, UserID}
import com.clemble.loveit.payment.service.PaymentServiceTestExecutor
import com.clemble.loveit.thank.service.repository.ThankRepository
import org.apache.commons.lang3.RandomStringUtils._
import org.junit.runner.RunWith
import org.specs2.concurrent.ExecutionEnv
import org.specs2.runner.JUnitRunner

@RunWith(classOf[JUnitRunner])
class UserSupportedProjectServiceSpec(implicit val ee: ExecutionEnv) extends PaymentServiceTestExecutor {

  val thankService = dependency[ThankService]
  val thankRepo = dependency[ThankRepository]
  val supportedProjectService = dependency[UserSupportedProjectsService]

  def createScene():(Resource, UserID, UserID) = {
    val url = HttpResource(s"example.com/some/${randomNumeric(10)}")
    // TODO flow must be changed here to use ResourceOwnersip verification
    val owner = createUser()
    await(roService.assignOwnership(owner, url))
    await(thankRepo.updateOwner(owner, url))
    val giver = createUser()

    (url, owner, giver)
  }

  def thank(user: UserID, url: Resource) = {
    await(thankService.thank(user, url))
  }

  def getSupported(user: UserID) = {
    await(supportedProjectService.getSupported(user));
  }

  "Supported projects " should {

    "be initialized on Thank" in {
      val (url, owner, giver) = createScene()

      getSupported(giver) shouldEqual Nil
      getSupported(owner) shouldEqual Nil

      thank(giver, url)

      getSupported(owner) shouldEqual Nil
      eventually(getSupported(giver) shouldEqual List(owner))
    }

  }

}