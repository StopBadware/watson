package models

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._
import java.net.URISyntaxException
import scala.util.Random
import scala.actors.Futures.future

@RunWith(classOf[JUnitRunner])
class UriSpec extends Specification {
  
  def validUri = UriSpec.validUri
  def reportedUri = new ReportedUri(validUri)
  private val source = Source.SBW
  
  "Uri" should {
    
    "create a new Uri" in {
      running(FakeApplication()) {
      	Uri.create(reportedUri) must beTrue
      	Uri.create(validUri) must beTrue
      }
    }
    
    "create new Uri in bulk" in {
      running(FakeApplication()) {
        val numInBulk = 100
        val uris = (1 to numInBulk).foldLeft(List.empty[ReportedUri]) { (list, _) =>
          list :+ reportedUri
        }
      	Uri.create(uris) must be equalTo(numInBulk)
      }
    }    
    
    "find an existing Uri by SHA2-256" in {
      running(FakeApplication()) {
        val reported = reportedUri
      	Uri.create(reported) must beTrue
      	Uri.find(reported.sha256) must beSome
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
    		val found = Uri.find(shas)
    		found.size must be equalTo(shas.size)
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
      	val uriStr = validUri
      	val sha = new ReportedUri(uriStr).sha256
        Uri.find(sha) must beNone
      	Uri.findOrCreate(uriStr) must beSome
      	Uri.find(sha) must beSome
      }
    }
    
    "find or create a Uri concurrently" in {
      running(FakeApplication()) {
        val reported = reportedUri
        Uri.find(reported.sha256) must beNone
        for (i <- 1 to 50) {
        	future(Uri.findOrCreate(reported) must beSome)
        }
        Thread.sleep(2000) //wait for all futures to complete
        Uri.find(reported.sha256) must beSome
      }
    }
    
    "find or create in bulk" in {
      running(FakeApplication()) {
    		val numInBulk = 10
        val uris = (1 to numInBulk).foldLeft(List.empty[String]) { (list, _) =>
          list :+ validUri
        }
    		val found = Uri.findOrCreate(uris)
    		found.size must be equalTo(uris.size)
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
  
  def validUri: String = "https://example.com/some/path?q=query#fragment" + System.currentTimeMillis + Random.nextInt
  
}