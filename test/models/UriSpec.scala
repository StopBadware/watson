package models

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._
import java.net.URISyntaxException
import scala.util.Random
import scala.actors.Futures.future
import controllers.Hash
import models.enums.Source

@RunWith(classOf[JUnitRunner])
class UriSpec extends Specification {
  
  def validUri = UriSpec.validUri
  def reportedUri = new ReportedUri(validUri)
  private val source = Source.NSF
  
  "Uri" should {
    
    "create a new Uri" in {
      running(FakeApplication()) {
      	Uri.create(reportedUri) must beTrue
      	Uri.create(validUri) must beTrue
      }
    }
    
    "add slash if no explicit path in Uri" in {
      running(FakeApplication()) {
        val noSlash = "http://example"+Random.nextInt+".com"
      	Uri.create(noSlash) must beTrue
      	Uri.findBySha256(Hash.sha256(noSlash).get) must beNone
      	Uri.findBySha256(Hash.sha256(noSlash+"/").get) must beSome
      	val noSlashNoScheme = "example"+Random.nextInt+".com"
      	Uri.create(noSlashNoScheme) must beTrue
      	Uri.findBySha256(Hash.sha256("http://"+noSlashNoScheme).get) must beNone
      	Uri.findBySha256(Hash.sha256("http://"+noSlashNoScheme+"/").get) must beSome
      }
    }
    
    "create new Uri in bulk" in {
      running(FakeApplication()) {
        val numInBulk = 100
        val uris = (1 to numInBulk).foldLeft(List.empty[String]) { (list, _) =>
          list :+ validUri
        }
      	Uri.create(uris) must equalTo(numInBulk)
      }
    } 
    
    "find an existing Uri by ID" in {
      running(FakeApplication()) {
        val reported = reportedUri
      	Uri.create(reported) must beTrue
      	val id = Uri.findBySha256(reported.sha256).get.id
      	Uri.find(id) must beSome
      }
    }
    
    "find existing Uris by IDs" in {
      running(FakeApplication()) {
        val uris = (1 to 10).foldLeft(List.empty[String])((list, _) => list :+ validUri)
        val ids = Uri.findOrCreateIds(uris)
        ids.size must equalTo(uris.size)
        val found = Uri.find(ids)
        found.nonEmpty must beTrue
        found.size must equalTo(uris.size)
        found.map(uri => ids.contains(uri.id) must beTrue)
      }
    }
    
    "find an existing Uri by SHA2-256" in {
      running(FakeApplication()) {
        val reported = reportedUri
      	Uri.create(reported) must beTrue
      	Uri.findBySha256(reported.sha256) must beSome
      }
    }
    
    "find Uris by SHA2-256 in bulk" in {
      running(FakeApplication()) {
    		val numInBulk = 10
        val shas = (1 to numInBulk).foldLeft(List.empty[String]) { (list, _) =>
          val rep = reportedUri
          if (Uri.create(rep)) list :+ rep.sha256 else list
        }
    		shas.nonEmpty must beTrue
    		val found = Uri.findBySha256(shas)
    		found.size must equalTo(shas.size)
      }
    }     
    
    "find or create a Uri" in {
      running(FakeApplication()) {
        val reported = reportedUri
        Uri.findBySha256(reported.sha256) must beNone
      	Uri.findOrCreate(reported) must beSome
      	Uri.findBySha256(reported.sha256) must beSome
      	val uriStr = validUri
      	val sha = new ReportedUri(uriStr).sha256
        Uri.findBySha256(sha) must beNone
      	Uri.findOrCreate(uriStr) must beSome
      	Uri.findBySha256(sha) must beSome
      }
    }
    
    "find or create a Uri concurrently" in {
      running(FakeApplication()) {
        val reported = reportedUri
        Uri.findBySha256(reported.sha256) must beNone
        for (i <- 1 to 50) {
        	future(Uri.findOrCreate(reported) must beSome)
        }
        Thread.sleep(2000) //wait for all futures to complete
        Uri.findBySha256(reported.sha256) must beSome
      }
    }
    
    "find or create in bulk" in {
      running(FakeApplication()) {
    		val numInBulk = 10
        val uris = (1 to numInBulk).foldLeft(List.empty[String]) { (list, _) =>
          list :+ validUri
        }
    		val found = Uri.findOrCreateIds(uris)
    		found.size must equalTo(uris.size)
      }
    }       
    
    "remove a Uri" in {
      running(FakeApplication()) {
        val reported = reportedUri
        val uri = Uri.findOrCreate(reported)
        uri must beSome
        uri.get.delete()
        Uri.findBySha256(reported.sha256) must beNone
      }
    }
    
    "check if a Uri is blacklisted by any source" in {
      running(FakeApplication()) {
        val reported = reportedUri
        Uri.create(reported) must beTrue
        val found = Uri.findBySha256(reported.sha256)
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
        val found = Uri.findBySha256(reported.sha256)
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
        BlacklistEvent.findByUri(uri.id).size must equalTo(0)
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
        BlacklistEvent.findBlacklistedByUri(uri.id, Some(source)).size must equalTo(0)
      }
    }
    
    "request a review" in {
      running(FakeApplication()) {
        val uri = Uri.findOrCreate(reportedUri).get
        ReviewRequest.findByUri(uri.id).isEmpty must beTrue
        uri.requestReview("orgrim.doomhammer@example.com") must beTrue
        ReviewRequest.findByUri(uri.id).nonEmpty must beTrue
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
  
  def validUri: String = "https://example.com/some/path?q=query#fragment" + System.currentTimeMillis + Random.nextInt
  
}