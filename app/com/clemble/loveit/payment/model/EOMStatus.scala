package com.clemble.loveit.payment.model

import java.time.{LocalDateTime, YearMonth}

import com.clemble.loveit.common.model.CreatedAware
import com.clemble.loveit.common.util.WriteableUtils
import play.api.libs.json.Json

/**
  * EOM statistics
  *
  * @param total number of all subjects for processing
  * @param pending number of pending subjects for processing
  * @param success number of successfully processed
  * @param failed number of failed processings
  */
case class EOMStatistics(
                    total: Long = 0,
                    pending: Long = 0,
                    success: Long = 0,
                    failed: Long = 0
                  ) {

  def isValid = total == (pending + success + failed)

  def incSuccess() = copy(total = total + 1, success = success + 1)
  def incFailure() = copy(total = total + 1, failed = failed + 1)
}

object EOMStatistics {
  implicit val statJsonFormat = Json.format[EOMStatistics]
}

/**
  * EOM processing status generator
  *
  * @param createCharges statistics for applying charges
  * @param applyCharges statistics for applying charges, total is number of success from previous step
  * @param createPayout statistics for generated payouts
  * @param applyPayout statistics for applying payouts
  * @param created creation time
  */
case class EOMStatus(
                             yom: YearMonth,
                             createCharges: Option[EOMStatistics] = None,
                             applyCharges: Option[EOMStatistics] = None,
                             createPayout: Option[EOMStatistics] = None,
                             applyPayout: Option[EOMStatistics] = None,
                             finished: Option[LocalDateTime] = None,
                             created: LocalDateTime = LocalDateTime.now()
              ) extends CreatedAware with EOMAware{

  def isValid = createCharges.map(_.isValid).getOrElse(false) &&
    applyCharges.map(_.isValid).getOrElse(false) &&
    createPayout.map(_.isValid).getOrElse(false) &&
    applyPayout.map(_.isValid).getOrElse(false)

}

object EOMStatus {

  implicit val jsonFormat = Json.format[EOMStatus]

  implicit val writeableFormat = WriteableUtils.jsonToWriteable[EOMStatus]()

}
