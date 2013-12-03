package models

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._
import java.net.{URI, URISyntaxException}

@RunWith(classOf[JUnitRunner])
class UriSpec extends Specification {
  
  private val validUri = "https://example.com/some/path?q=query&a=another#fragment"+System.currentTimeMillis
  private val reported = new ReportedUri(validUri)
  
  "Uri" should {
    
    "add a new Uri" in {
      running(FakeApplication()) {
      	Uri.create(reported) must be equalTo(true)
      	Uri.find(reported.sha256) must beSome
      }
    }
    
  }
  
  "ReportedUri" should {
    
    "not throw a URISyntaxException from a valid URI" in {
      new ReportedUri(validUri) must beAnInstanceOf[ReportedUri]
    }
    
    "throw a URISyntaxException from an invalid URI" in {
      val invalidURI = "For the Horde!"
      new ReportedUri(invalidURI) must throwA[URISyntaxException]
    }
    
  }

}