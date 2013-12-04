package models

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._
import java.net.{URI, URISyntaxException}
import scala.util.Random

@RunWith(classOf[JUnitRunner])
class UriSpec extends Specification {
  
//  private val validUri = "https://example.com/some/path?q=query#fragment"+System.currentTimeMillis
  private val validUri = "https://example.com"
  
  "Uri" should {
    
    "create a new Uri" in {
      running(FakeApplication()) {
        val reported = new ReportedUri(validUri+Random.nextInt)
      	Uri.create(reported) must be equalTo(true)
      }
    }
    
    "find an existing Uri" in {
      running(FakeApplication()) {
        val reported = new ReportedUri(validUri+Random.nextInt)
      	Uri.create(reported) must be equalTo(true)
      	Uri.find(reported.sha256) must beSome
      }
    }
    
    "find or create a Uri" in {
      running(FakeApplication()) {
        val reported = new ReportedUri(validUri+Random.nextInt)
        Uri.find(reported.sha256) must beNone
      	Uri.findOrCreate(reported) must beSome
      }
    }
    
    "remove a Uri" in {
      running(FakeApplication()) {
        val reported = new ReportedUri(validUri+Random.nextInt)
        val uri = Uri.findOrCreate(reported)
        uri must beSome
        uri.get.delete()
        Uri.find(reported.sha256) must beNone
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