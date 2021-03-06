package com.clemble.loveit.common.model

import play.api.libs.json.Json

/**
  * Thank abstraction
  */
case class Thank(
                  given: Amount = 0L,
                  supporters: Set[UserID] = Set.empty,
                ) {


  def isSupportedBy(user: UserID): Boolean = supporters.contains(user)

  def withSupporter(supporter: UserID): Thank = {
    Thank(supporters = supporters + supporter, given = given + 1)
  }

}

object Thank {

  /**
    * JSON format for [[Thank]]
    */
  implicit val jsonFormat = Json.format[Thank]

}