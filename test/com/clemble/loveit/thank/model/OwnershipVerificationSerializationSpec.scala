package com.clemble.loveit.thank.model

import com.clemble.loveit.common.SerializationSpec
import com.clemble.loveit.test.util.{Generator, OwnershipVerificationGenerator}
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import play.api.libs.json.Format

@RunWith(classOf[JUnitRunner])
class OwnershipVerificationSerializationSpec extends SerializationSpec[OwnershipVerificationRequest] {

  override val generator: Generator[OwnershipVerificationRequest] = OwnershipVerificationGenerator
  override val jsonFormat: Format[OwnershipVerificationRequest] = OwnershipVerificationRequest.jsonFormat

}