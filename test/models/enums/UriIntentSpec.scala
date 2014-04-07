package models.enums

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class UriIntentSpec extends Specification {
  
  private val intent = "hacked"
  
  "UriIntent" should {
    
    "match string to UriIntent" in {
      UriIntent.fromStr("") must beNone
      UriIntent.fromStr(intent.toLowerCase) must beSome
      UriIntent.fromStr(intent.toUpperCase) must beSome
      val uriIntent = UriIntent.fromStr(intent).get
      uriIntent.toString.equalsIgnoreCase(intent)
      uriIntent.equals(UriIntent.HACKED) must beTrue
      uriIntent.equals(UriIntent.MALICIOUS) must beFalse
    }
    
  }

}