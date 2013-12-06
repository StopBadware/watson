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
  private val source = Source.SBW
  
  "Uri" should {
    
    "create a new Uri" in {
      running(FakeApplication()) {
        val reported = reportedUri
      	Uri.create(reported) must beTrue
      }
    }
    
    "find an existing Uri by SHA2-256" in {
      running(FakeApplication()) {
        val reported = reportedUri
      	Uri.create(reported) must beTrue
      	Uri.find(reported.sha256) must beSome
      }
    }
    
    "find Uris by hierarchical part" in {
      running(FakeApplication()) {
        val reported = reportedUri
      	Uri.create(reported) must beTrue
      	val foundByHp = Uri.findByHierarchicalPart(reported.hierarchicalPart)
      	foundByHp.nonEmpty must beTrue
      	val foundBySha = Uri.find(reported.sha256)
      	foundBySha must beSome
      	foundByHp.contains(foundBySha.get) must beTrue
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
        Uri.create(reported) must beTrue
        val found = Uri.find(reported.sha256)
        found must beSome
        val uri = found.get
        uri.blacklist(source, System.currentTimeMillis/1000)
        uri.isBlacklisted must beTrue
        uri.removeFromBlacklist(source, System.currentTimeMillis/1000) must beTrue
        uri.isBlacklisted must beFalse
      }
    }
    
    "check if a Uri is blacklisted by specific source" in {
      running(FakeApplication()) {
        val reported = reportedUri
        Uri.create(reported) must beTrue
        val found = Uri.find(reported.sha256)
        found must beSome
        val uri = found.get
        uri.blacklist(source, System.currentTimeMillis/1000)
        uri.isBlacklistedBy(source) must beTrue
        uri.removeFromBlacklist(source, System.currentTimeMillis/1000) must beTrue
        uri.isBlacklistedBy(source) must beFalse
      }
    }
    
    "mark a Uri as blacklisted" in {
      running(FakeApplication()) {
        val reported = reportedUri
        val created = Uri.findOrCreate(reported)
        created must beSome
        val uri = created.get
        BlacklistEvent.findByUri(uri.id).size must be equalTo(0)
        uri.blacklist(source, uri.createdAt) must beTrue
        BlacklistEvent.findByUri(uri.id).size must be_>(0)
      }
    }
    
    "mark a Uri as no longer blacklisted" in {
      running(FakeApplication()) {
        val reported = reportedUri
        val created = Uri.findOrCreate(reported)
        created must beSome
        val uri = created.get
        uri.blacklist(source, uri.createdAt) must beTrue
        BlacklistEvent.findByUri(uri.id, Some(source)).size must be_>(0)
        uri.removeFromBlacklist(source, System.currentTimeMillis/1000)
        BlacklistEvent.findBlacklistedByUri(uri.id, Some(source)).size must be equalTo(0)
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