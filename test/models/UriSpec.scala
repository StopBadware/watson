package models

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import java.net.{URI, URISyntaxException}
import com.mongodb.casbah.Imports._

class UriSpec extends Specification {
  
  private val validUri = "https://example.com/some/path?q=query&a=another#fragment"
  
  "a Uri" should {
    
    val reported = new ReportedUri(validUri)
    val uriDoc = controllers.DbHandler.findOrCreate(reported)
    
    "map to a document in the uris collection" in {
      uriDoc.isDefined must beTrue
      val uri = Uri(uriDoc.get)
      uri must beAnInstanceOf[Uri]
    }
    
  }
  
  "a ReportedUri" should {
    
    "not throw a URISyntaxException from a valid URI" in {
      new ReportedUri(validUri) must beAnInstanceOf[ReportedUri]
    }
    
    "throw a URISyntaxException from an invalid URI" in {
      val invalidURI = "For the Horde!"
      new ReportedUri(invalidURI) must throwA[URISyntaxException]
    }
    
  }

}