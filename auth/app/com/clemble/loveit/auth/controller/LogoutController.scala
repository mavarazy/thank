package com.clemble.loveit.auth.controller

import javax.inject.Inject

import com.clemble.loveit.common.controller.CookieUtils
import com.clemble.loveit.common.util.AuthEnv
import com.mohiva.play.silhouette.api.Silhouette
import play.api.mvc.{AbstractController, ControllerComponents}

class LogoutController @Inject() (
                                   silhouette: Silhouette[AuthEnv],
                                   cookieUtils: CookieUtils,
                                   components: ControllerComponents
                                 ) extends AbstractController(components) {

  def logout() = silhouette.UnsecuredAction(_ => {
    Redirect("/").withCookies(cookieUtils.removeUser())
  })

}
