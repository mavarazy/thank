package com.clemble.loveit.payment.service.repository

import com.clemble.loveit.common.RepositorySpec
import com.clemble.loveit.common.model.{Resource, ThankTransaction, UserID}
import org.junit.runner.RunWith
import org.specs2.concurrent.ExecutionEnv
import org.specs2.runner.JUnitRunner

import scala.collection.immutable.Seq

@RunWith(classOf[JUnitRunner])
class ThankTransactionRepositorySpec(implicit ee: ExecutionEnv) extends RepositorySpec {

  val repo = dependency[ThankTransactionRepository]

  "CREATE" should {

    "same resource transactions saved only once" in {
      val user = createUser()
      val res = someRandom[Resource]
      val A = ThankTransaction(user, someRandom[UserID], res)
      val B = ThankTransaction(user, someRandom[UserID], res)

      await(repo.save(A))
      await(repo.save(B))

      val userTransactions = repo.findByUser(user).toSeq()
      userTransactions.size shouldEqual 1
    }

    "save all payments for the user" in {
      val user = createUser()
      val A = ThankTransaction(user, someRandom[UserID], someRandom[Resource])
      val B = ThankTransaction(user, someRandom[UserID], someRandom[Resource])

      await(repo.save(A))
      await(repo.save(B))
      val transactions = repo.findByUser(user).toSeq

      transactions must containAllOf(Seq(A, B)).exactly
    }

    "remove specified" in {
      val user = createUser()
      val A = ThankTransaction(user, someRandom[UserID], someRandom[Resource])
      val B = ThankTransaction(user, someRandom[UserID], someRandom[Resource])

      await(repo.save(A))
      await(repo.save(B))

      await(repo.removeAll(Seq(A)))

      val afterRemove = repo.findByUser(user).toSeq
      afterRemove shouldEqual Seq(B)
    }

  }

}
