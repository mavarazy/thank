package com.clemble.loveit.thank.service.repository

import java.time.YearMonth

import com.clemble.loveit.common.model.UserID
import com.clemble.loveit.thank.model.UserStat

import scala.concurrent.Future

trait UserStatRepo {

  def increase(owner: UserID): Future[Boolean]

  def get(user: UserID, yearMonth: YearMonth): Future[UserStat]

}
