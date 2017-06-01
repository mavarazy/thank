package com.clemble.loveit.thank.controller

import javax.inject.Inject

import com.clemble.loveit.common.model.Resource
import com.clemble.loveit.common.util.AuthEnv
import com.clemble.loveit.thank.model.VerificationID
import com.clemble.loveit.thank.service.ROVerificationService
import com.mohiva.play.silhouette.api.Silhouette
import play.api.libs.json.Json
import play.api.mvc.Controller

import scala.concurrent.ExecutionContext

case class ROVerificationController @Inject()(
                                               service: ROVerificationService,
                                               silhouette: Silhouette[AuthEnv],
                                               implicit val ec: ExecutionContext
                                                    ) extends Controller {

  def getMy(verID: VerificationID) = silhouette.SecuredAction.async(implicit req => {
    val fVerification = service.get(req.identity.id, verID)
    fVerification.map(_ match {
      case Some(res) => Ok(res)
      case None => NotFound
    })
  })

  def listMy = silhouette.SecuredAction.async(implicit req => {
    val fVerifications = service.list(req.identity.id)
    fVerifications.map(Ok(_))
  })

  def removeMy(verID: VerificationID) = silhouette.SecuredAction.async(implicit req => {
    val fRemove = service.remove(req.identity.id, verID)
    fRemove.map(res => Ok(Json.toJson(res)))
  })

  def verifyMy(verID: VerificationID) = silhouette.SecuredAction.async(implicit req => {
    val fVerification = service.verify(req.identity.id, verID)
    fVerification.map(_ match {
      case Some(res) => Ok(res)
      case None => NotFound
    })
  })

  def createMy() = silhouette.SecuredAction.async(parse.json[Resource])(implicit req => {
    val fVerification = service.create(req.identity.id, req.body)
    fVerification.map(Created(_))
  })

}