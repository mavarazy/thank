package com.clemble.loveit.auth

import javax.inject.Singleton

import com.clemble.loveit.auth.service.repository.AuthTokenRepository
import com.clemble.loveit.auth.service.repository.mongo.MongoAuthTokenRepository
import com.clemble.loveit.auth.service.{AuthTokenService, SimpleAuthTokenService}
import com.clemble.loveit.common.mongo.JSONCollectionFactory
import com.google.inject.name.Named
import com.google.inject.{AbstractModule, Provides}
import net.codingwell.scalaguice.ScalaModule
import org.matthicks.mailgun.Mailgun
import play.api
import play.api.{Configuration}
import play.modules.reactivemongo.ReactiveMongoApi

import scala.concurrent.ExecutionContext

/**
  * The base Guice module.
  */
class AuthValidationModule(env: api.Environment, conf: Configuration) extends AbstractModule with ScalaModule {

  /**
    * Configures the module.
    */
  def configure(): Unit = {
    bind[AuthTokenRepository].to[MongoAuthTokenRepository]
    bind[AuthTokenService].to[SimpleAuthTokenService]
  }

  @Provides
  @Singleton
  @Named("authToken")
  def authTokenCollection(mongoApi: ReactiveMongoApi, ec: ExecutionContext) = {
    JSONCollectionFactory.create("authInfo", mongoApi, ec, env)
  }

  @Provides
  @Singleton
  def mailgun(): Mailgun = {
    new Mailgun(conf.get[String]("EMAIL_MAILGUN_DOMAIN"), conf.get[String]("EMAIL_MAILGUN_API_KEY"))
  }

}
