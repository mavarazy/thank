package com.clemble.loveit.payment

import java.util.Currency

import com.braintreegateway.BraintreeGateway
import com.clemble.loveit.model.Amount
import com.clemble.loveit.payment.model.BankDetails
import com.clemble.loveit.payment.service.repository.PaymentTransactionRepository
import com.clemble.loveit.payment.service.repository.mongo.MongoPaymentTransactionRepository
import com.clemble.loveit.payment.service._
import com.clemble.loveit.util.LoveItCurrency
import com.google.inject.Provides
import com.google.inject.name.Named
import com.paypal.base.rest.APIContext
import net.codingwell.scalaguice.ScalaModule
import play.api.Configuration
import play.modules.reactivemongo.ReactiveMongoApi
import reactivemongo.api.FailoverStrategy
import reactivemongo.play.json.collection.JSONCollection

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

class PaymentModule extends ScalaModule {

  override def configure() = {
    bind[PaymentTransactionRepository].to[MongoPaymentTransactionRepository]
    bind[BraintreeService].to[SimpleBraintreeService]

    val currencyToAmount: Map[Currency, Amount] = Map[Currency, Amount](LoveItCurrency.getInstance("USD") -> 10L)
    bind[ExchangeService].toInstance(InMemoryExchangeService(currencyToAmount))
    bind[PaymentTransactionService].to[SimplePaymentTransactionService]
  }

  @Provides
  def apiContext(configuration: Configuration): APIContext = {
    val clientId = configuration.getString("payment.payPal.rest.clientId").get
    val clientSecret = configuration.getString("payment.payPal.rest.clientSecret").get
    val mode = configuration.getString("payment.payPal.rest.mode").get
    new APIContext(clientId, clientSecret, mode)
  }

  @Provides
  def withdrawService(context: APIContext): WithdrawService[BankDetails] = {
    WithdrawServiceFacade(PayPalWithdrawService(context))
  }

  @Provides
  def braintreeGateway(configuration: Configuration): BraintreeGateway = {
    val tokeOpt = configuration.getString("payment.braintree.token")
    new BraintreeGateway(tokeOpt.get)
  }

  @Provides
  @Named("paymentTransactions")
  def paymentTransactionMongoCollection(mongoApi: ReactiveMongoApi, ec: ExecutionContext): JSONCollection = {
    val fCollection: Future[JSONCollection] = mongoApi.
      database.
      map(_.collection[JSONCollection]("paymentTransaction", FailoverStrategy.default))(ec)
    Await.result(fCollection, 1 minute)
  }

}
