package internbookmark.helper

import org.scalatest._
import org.scalatra.test.scalatest.ScalatraSpec

abstract class WebUnitSpec extends ScalatraSpec with Matchers with
OptionValues with Inside with Inspectors
