package models.enums

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class UriTypeSpec extends Specification {
  
  val testType = "payload"
  
  "UriType" should {
    
    "match string to UriType" in {
      UriType.fromStr("") must beNone
      UriType.fromStr(testType.toLowerCase) must beSome
      UriType.fromStr(testType.toUpperCase) must beSome
      val uriType = UriType.fromStr(testType).get
      uriType.toString.equalsIgnoreCase(testType)
      uriType.equals(UriType.PAYLOAD) must beTrue
      uriType.equals(UriType.INTERMEDIARY) must beFalse
    }
    
  }

}