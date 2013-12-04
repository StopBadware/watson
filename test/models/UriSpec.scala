package models

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._
import java.net.URISyntaxException
import scala.util.Random

@RunWith(classOf[JUnitRunner])
class UriSpec extends Specification {
  
  def reportedUri = UriSpec.reportedUri
  
  "Uri" should {
    
    "create a new Uri" in {
      running(FakeApplication()) {
        val reported = reportedUri
      	Uri.create(reported) must be equalTo(true)
      }
    }
    
    "find an existing Uri" in {
      running(FakeApplication()) {
        val reported = reportedUri
      	Uri.create(reported) must be equalTo(true)
      	Uri.find(reported.sha256) must beSome
      }
    }
    
    "find or create a Uri" in {
      running(FakeApplication()) {
        val reported = reportedUri
        Uri.find(reported.sha256) must beNone
      	Uri.findOrCreate(reported) must beSome
      	Uri.find(reported.sha256) must beSome
      }
    }
    
    "remove a Uri" in {
      running(FakeApplication()) {
        val reported = reportedUri
        val uri = Uri.findOrCreate(reported)
        uri must beSome
        uri.get.delete()
        Uri.find(reported.sha256) must beNone
      }
    }
    
    "check if a Uri is blacklisted by any source" in {
      running(FakeApplication()) {
        val reported = reportedUri
        Uri.create(reported) must be equalTo(true)
        val uri = Uri.find(reported.sha256)
        uri must beSome
        uri.get.isBlacklisted must be equalTo(false)
      }
    }
    
    "check if a Uri is blacklisted by specific source" in {
      running(FakeApplication()) {
        val reported = reportedUri
        Uri.create(reported) must be equalTo(true)
        val uri = Uri.find(reported.sha256)
        uri must beSome
        uri.get.isBlacklisted must be equalTo(false)
      }
    }
    
    "mark a Uri as blacklisted" in {
      running(FakeApplication()) {
        val reported = reportedUri
        val uri = Uri.findOrCreate(reported)
        uri must beSome
        //TODO WTSN-11
      }
    }
    
  }
  
  "ReportedUri" should {
    
    "not throw a URISyntaxException from a valid URI" in {
      new ReportedUri(UriSpec.validUri) must beAnInstanceOf[ReportedUri]
    }
    
    "throw a URISyntaxException from an invalid URI" in {
      val invalidURI = "For the Horde!"
      new ReportedUri(invalidURI) must throwA[URISyntaxException]
    }
    
  }

}

object UriSpec {
  
  val validUri = "https://example.com/some/path?q=query#fragment" + System.currentTimeMillis
  def reportedUri: ReportedUri = new ReportedUri(validUri + Random.nextInt)
  
}