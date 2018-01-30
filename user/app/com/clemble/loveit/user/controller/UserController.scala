package com.clemble.loveit.user.controller

import com.clemble.loveit.user.service.UserService
import com.clemble.loveit.common.util.AuthEnv
import javax.inject.{Inject, Singleton}

import com.clemble.loveit.common.controller.LoveItController
import com.clemble.loveit.common.model.UserID
import com.clemble.loveit.user.model.User
import com.mohiva.play.silhouette.api.Silhouette
import play.api.mvc.ControllerComponents

import scala.concurrent.{ExecutionContext}

@Singleton
case class UserController @Inject()(
                                     userService: UserService,
                                     silhouette: Silhouette[AuthEnv],
                                     components: ControllerComponents,
                                     implicit val ec: ExecutionContext
                                   ) extends LoveItController(components) {

  def get(user: UserID) = silhouette.SecuredAction.async(implicit req => {
    val realId = idOrMe(user)
    val fUserOpt = userService.findById(realId)
    fUserOpt.map(userOpt => Ok(userOpt.get))
  })

  def updateMyProfile() = silhouette.SecuredAction.async(parse.json[User])(implicit req => {
    val user = req.body.copy(id = req.identity.id)
    userService.update(user).map(Ok(_))
  })
}
