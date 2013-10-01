package models

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import java.net.{URI, URISyntaxException}
import com.mongodb.casbah.Imports._

class UriSpec extends Specification {
  
  private val validUri = "https://example.com/some/path?q=query&a=another#fragment"
  
  "a URI" should {
    
  }
  
  "a ReportedURI" should {
    
    "not throw a URISyntaxException from a valid URI" in {
      new ReportedUri(validUri) must beAnInstanceOf[ReportedUri]
    }
    
    "throw a URISyntaxException from an invalid URI" in {
      val invalidURI = "For the Horde!"
      new ReportedUri(invalidURI) must throwA[URISyntaxException]
    }
    
  }

}