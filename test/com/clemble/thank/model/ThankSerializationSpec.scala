package com.clemble.thank.model

import com.clemble.thank.test.util.{Generator, ThankGenerator}
import org.junit.runner.RunWith
import org.specs2.runner.JUnitRunner
import play.api.libs.json.Format

@RunWith(classOf[JUnitRunner])
class ThankSerializationSpec extends SerializationSpec[Thank] {

  override val generator: Generator[Thank] = ThankGenerator
  override val jsonFormat: Format[Thank] = Thank.jsonFormat

  "with parents" should {

    "return all" in {
      val thank = Thank("example.com/some/what")
      val expected = List(
        Thank("example.com/some/what"),
        Thank("example.com/some"),
        Thank("example.com")
      )
      thank.withParents() shouldEqual expected
    }

  }
}
