package controllers

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._
import java.net.URI
import scala.util.Random
import com.codahale.jerkson.Json
import models._

@RunWith(classOf[JUnitRunner])
class BlacklistSpec extends Specification {
  
  sequential	//differential blacklists tests running in parallel can affect each other 
  private val invalidUrl = "http://example.com/invalid\\\\path"
  private val source = Source.GOOG
  
  private def mostRecentTime: Long = BlacklistEvent.timeOfLast(source)
  
  private def validUrl: String = "example" + Random.nextInt + ".com/" + (System.currentTimeMillis / 1000)
  
  private def find(url: String): Uri = Uri.find(new ReportedUri(url).sha256).get
  
  private def blacklist: Blacklist = {
    val urls = (1 to 5).foldLeft(List.empty[String]) { (list, _) =>
      list :+ validUrl
    }
    Blacklist(source, System.currentTimeMillis / 1000, urls)
  }
  
  private def isBlacklisted(url: String): Boolean = {
    BlacklistEvent.findBlacklistedByUri(find(url).id, Some(source)).nonEmpty
  }
  
  "Blacklist" should {
    
    "add differential blacklist to queue" in {
      running(FakeApplication()) {
        val time = System.currentTimeMillis / 1000
        val urlA = "example"+time+".com"
        val urlB = "https://example.com/" + time
	      Blacklist.importBlacklist(BlacklistSpec.json(time, List(urlA, urlB, invalidUrl)), source)
	      Redis.getBlacklist(source, time).nonEmpty must beTrue
      }
    }    
    
    "add new entries from differential blacklist" in {
      running(FakeApplication()) {
        val bl = blacklist
        val existingUrl = bl.urls.head
        Uri.findOrCreate(existingUrl) must beSome
	      Blacklist.importDifferential(bl.urls, source, mostRecentTime)
	      bl.urls.map(isBlacklisted(_) must beTrue)
      }
    }
    
    "update entries differential entries blacklist (imported in order)" in {
      running(FakeApplication()) {
        val timeA = mostRecentTime
	      val timeB = timeA + 10
	      
	      val bl = blacklist
	      val falloffUrl = validUrl
	      Blacklist.importDifferential(bl.urls :+ falloffUrl, source, timeA)
	      bl.urls.map(isBlacklisted(_) must beTrue)
        isBlacklisted(falloffUrl) must beTrue
        
        Blacklist.importDifferential(bl.urls, source, timeB)
        bl.urls.map(isBlacklisted(_) must beTrue)
        isBlacklisted(falloffUrl) must beFalse
      }
    }
    
    "update differential blacklist entries (imported out of order)" in {
      running(FakeApplication()) {
        val timeA = mostRecentTime
	      val timeB = timeA + 10
	      val bl = blacklist
	      val falloffUrl = validUrl
	      
	      Blacklist.importDifferential(bl.urls, source, timeB)
        bl.urls.map(isBlacklisted(_) must beTrue)
        
	      Blacklist.importDifferential(bl.urls :+ falloffUrl, source, timeA)
	      bl.urls.map { url =>
          isBlacklisted(url) must beTrue
          BlacklistEvent.findByUri(find(url).id, Some(source)).map(_.blacklistedAt must equalTo(timeA))
        }
        isBlacklisted(falloffUrl) must beFalse
        
      }
    }
    
    "import Google appeal results" in {
      running(FakeApplication()) {
        val time = System.currentTimeMillis / 1000
	      val badUrl = validUrl
	      val badLink = "http://" + badUrl + time
	      val cleanUrl = validUrl
	      val appealsJson = "[{\"url\":\""+badUrl+"\",\"status\":\"bad\",\"time\":"+time+",\"source\":\"autoappeal\",\"link\":\""+badLink+"\"},"+
	        "{\"url\":\""+cleanUrl+"\",\"status\":\"clean\",\"time\":"+time+",\"source\":\"autoappeal\"},"+
	        "{\"url\":\""+invalidUrl+"\",\"status\":\"bad\",\"time\":"+time+",\"source\":\"autoappeal\"}]"
	      Blacklist.importGoogleAppeals(appealsJson)
	      val bad = find(badUrl)
	      GoogleRescan.findByUri(bad.id).nonEmpty must beTrue
	      GoogleRescan.findByUri(bad.id).head.relatedUriId.get must equalTo(find(badLink).id)
	      GoogleRescan.findByUri(find(cleanUrl).id).nonEmpty must beTrue
      }
    }
    
    "import NSF blacklist" in {
      running(FakeApplication()) {
        val time = System.currentTimeMillis / 1000
        val cleanTime = time + 10
        val existingUrl = validUrl
        Uri.findOrCreate(existingUrl) must beSome
        val newUrl = validUrl
	      val nsfJson = "["+
	        	"{\"url\":\""+newUrl+"\",\"time\":"+time+",\"clean\":0},"+
	        	"{\"url\":\""+invalidUrl+"\",\"time\":"+time+",\"clean\":0},"+
	        	"{\"url\":\""+existingUrl+"\",\"time\":"+time+",\"clean\":"+cleanTime+"}"+
	        "]"
	      Blacklist.importBlacklist(nsfJson, Source.NSF)
	      BlacklistEvent.findBlacklistedByUri(find(newUrl).id, Some(Source.NSF)).nonEmpty must beTrue
	      BlacklistEvent.findByUri(find(existingUrl).id, Some(Source.NSF)).nonEmpty must beTrue
	      BlacklistEvent.findBlacklistedByUri(find(existingUrl).id, Some(Source.NSF)).isEmpty must beTrue
      }      
    }
    
  }

}

object BlacklistSpec {
  def json(time: Long, urls: List[String]): String = Json.generate(Map("time"->time, "blacklist"->urls))
}
