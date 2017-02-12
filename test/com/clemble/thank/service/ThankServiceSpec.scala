package com.clemble.thank.service

import com.clemble.thank.model.ResourceOwnership
import com.clemble.thank.test.util.UserGenerator
import com.clemble.thank.util.URIUtilsSpec
import org.specs2.concurrent.ExecutionEnv
import org.apache.commons.lang3.RandomStringUtils._

class ThankServiceSpec(implicit val ee: ExecutionEnv) extends ServiceSpec {

  val thankService = application.injector.instanceOf[ThankService]
  val userService = application.injector.instanceOf[UserService]

  "Thank " should {

    "Decrement for the giver" in {
      val url = s"http/example.com/some/${randomNumeric(10)}"

      val giver = UserGenerator.generate()

      val beforeThank = await(userService.create(giver))
      val thank = await(thankService.thank(giver.id, url))
      val afterThank = await(userService.get(giver.id).map(_.get))
      thank.given shouldEqual 1
      beforeThank.balance - 1 shouldEqual afterThank.balance
    }

    "Increment for the owner" in {
      val url = s"example.com/some/${randomNumeric(10)}"
      val owner = UserGenerator.generate(ResourceOwnership.full(url))
      val giver = UserGenerator.generate()

      await(userService.create(giver))
      val beforeThank = await(userService.create(owner))
      val thank = await(thankService.thank(giver.id, url))
      val afterThank = await(userService.get(owner.id).map(_.get))

      thank.given shouldEqual 1
      (beforeThank.balance + 1) shouldEqual afterThank.balance
    }

    "Increment for the owner with variations" in {
      val url = s"example.com/some/${randomNumeric(10)}"

      val allVariations = URIUtilsSpec.generateVariations(url)
      allVariations.length must beGreaterThanOrEqualTo(1)

      val owner = UserGenerator.generate(ResourceOwnership.full(url))
      val giver = UserGenerator.generate()

      await(userService.create(giver))
      val beforeThank = await(userService.create(owner))
      for {
        variation <- allVariations
      } yield {
        await(thankService.thank(giver.id, variation))
      }
      val afterThank = await(userService.get(owner.id).map(_.get))

      val thank = await(thankService.get(url))
      thank.given shouldEqual allVariations.length
      (beforeThank.balance + allVariations.length) shouldEqual afterThank.balance
    }

  }

}
