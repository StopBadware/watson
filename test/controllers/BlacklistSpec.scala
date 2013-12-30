package controllers

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._
import java.net.URI
import scala.util.Random
import models._

@RunWith(classOf[JUnitRunner])
class BlacklistSpec extends Specification {
  
  sequential	//differential blacklists tests running in parallel can affect each other 
  private val invalidUrl = "http://example.com/invalid\\\\path"
  private val source = Source.GOOG
  
  private def mostRecentTime: Long = BlacklistEvent.timeOfLast(source)
  private def validUrl: String = "example" + Random.nextInt + ".com/" + (System.currentTimeMillis / 1000) 
  private def blacklist: Blacklist = {
    val urls = (1 to 5).foldLeft(List.empty[String]) { (list, _) =>
      list :+ validUrl
    }
    Blacklist(source, System.currentTimeMillis / 1000, urls)
  }
  private def find(url: String): Uri = Uri.find(new ReportedUri(url).sha256).get
  
  "Blacklist" should {
    
    "add differential blacklist to queue" in {
      running(FakeApplication()) {
        val time = System.currentTimeMillis / 1000
        val urlA = "example"+time+".com"
        val urlB = "https://example.com/" + time
	      val json = "[{\"url\":\""+urlB+"\",\"time\":"+time+"},"+ 
	      					 "{\"url\":\""+urlA+"\",\"time\":"+time+"},"+
	      					 "{\"url\":\""+invalidUrl+"\",\"time\":"+time+"}]"
	      Blacklist.importBlacklist(json, source)
	      Redis.getBlacklist(source, time).nonEmpty must beTrue
      }
    }    
    
    "add new entries from differential blacklist" in {
      running(FakeApplication()) {
        val bl = blacklist
        val existingUrl = bl.urls.head
        Uri.findOrCreate(existingUrl) must beSome
	      Blacklist.importDifferential(bl.urls, source, mostRecentTime)
	      
	      bl.urls.map { url =>
        	val uri = find(url)
        	uri.isBlacklisted must beTrue
        	BlacklistEvent.findBlacklistedByUri(uri.id, Some(source)).nonEmpty must beTrue
        }
      }
    }
    
    "update entries that fall off differential blacklist (imported in order)" in {
      running(FakeApplication()) {
        val timeA = mostRecentTime
	      val timeB = timeA + 10
	      
//	      val urlA = "example.com/" + System.currentTimeMillis
//	      val uriA = new ReportedUri(urlA)
//        val urlB = "www.example.com/" + System.currentTimeMillis
//        val urisA = List(urlA, urlB)
//	      val urisB = List(urlB)
	      
	      val blEarlier = blacklist
	      val blLater = blacklist
	      
	      Blacklist.importDifferential(blEarlier.urls, source, timeA)
	      blEarlier.urls.map { url =>
          val found = find(url)
          val filtered = found.filter(_.sha256.equalsIgnoreCase(found.sha256))
        }
	      val foundA = Uri.findByHierarchicalPart(uriA.hierarchicalPart)
	      foundA.nonEmpty must beTrue
	      val filtered = foundA.filter(_.sha256.equalsIgnoreCase(uriA.sha256))
	      filtered.nonEmpty must beTrue
	      filtered.filter(_.isBlacklisted).nonEmpty must beTrue
	      filtered.filter(_.isBlacklisted).map { uri =>
        	BlacklistEvent.findBlacklistedByUri(uri.id, Some(source)).nonEmpty must beTrue
        }
        
        Blacklist.importDifferential(urisB, source, timeB)
	      val foundB = Uri.findByHierarchicalPart(uriA.hierarchicalPart)
	      foundB.nonEmpty must beTrue
        foundB.filter(_.sha256.equalsIgnoreCase(uriA.sha256)).nonEmpty must beTrue
	      foundB.filter(u => u.sha256.equalsIgnoreCase(uriA.sha256) && u.isBlacklisted).isEmpty must beTrue
	      foundB.filter(_.isBlacklisted).map { uri =>
        	BlacklistEvent.findBlacklistedByUri(uri.id, Some(source)).nonEmpty must beTrue
        }
      }
    }
    
    "update entries that fall off differential blacklist (imported out of order)" in {
      running(FakeApplication()) {
        val timeA = mostRecentTime
	      val timeB = timeA + 10
	      
	      val urlA = "foo.com/" + System.currentTimeMillis
	      val uriA = new ReportedUri(urlA)
        val urlB = "www.bar.com/" + System.currentTimeMillis
        val urisA = List(urlA, urlB)
	      val urisB = List(urlB)
	      
        Blacklist.importDifferential(urisB, source, timeB)
	      Blacklist.importDifferential(urisA, source, timeA)
	      
	      val found = Uri.findByHierarchicalPart(uriA.hierarchicalPart)
	      found.nonEmpty must beTrue
	      val filtered = found.filter(_.sha256.equalsIgnoreCase(uriA.sha256))
	      filtered.nonEmpty must beTrue
	      filtered.filter(_.isBlacklisted).isEmpty must beTrue
	      filtered.map { uri =>
        	BlacklistEvent.findBlacklistedByUri(uri.id, Some(source)).isEmpty must beTrue
        }
      }
    }      
    
    "import Google appeal results" in {
      running(FakeApplication()) {
        val time = System.currentTimeMillis / 1000
	      val badUrl = "example"+time+".com/"
	      val badLink = "http://" + badUrl + time
	      val cleanUrl = "cleanurl.com/"
	      val json = "[{\"url\":\""+badUrl+"\",\"status\":\"bad\",\"time\":"+time+",\"source\":\"autoappeal\",\"link\":\""+badLink+"\"},"+
	        "{\"url\":\""+cleanUrl+"\",\"status\":\"clean\",\"time\":"+time+",\"source\":\"autoappeal\"},"+
	        "{\"url\":\""+invalidUrl+"\",\"status\":\"bad\",\"time\":"+time+",\"source\":\"autoappeal\"}]"
	      Blacklist.importGoogleAppeals(json)
	      val bad = Uri.find(new ReportedUri(badUrl).sha256)
	      bad must beSome
	      GoogleRescan.findByUri(bad.get.id).nonEmpty must beTrue
	      val link = Uri.find(new ReportedUri(badLink).sha256)
	      link must beSome
	      GoogleRescan.findByUri(bad.get.id).head.relatedUriId.get must equalTo(link.get.id)
	      val clean = Uri.find(new ReportedUri(cleanUrl).sha256)
	      clean must beSome
	      GoogleRescan.findByUri(clean.get.id).nonEmpty must beTrue
      }
    }
    
    "import NSF blacklist" in {
      running(FakeApplication()) {
        val time = (System.currentTimeMillis / 1000) - 47
        val cleanTime = System.currentTimeMillis / 1000
        val existingUrl = "example"+time+".com"
        Uri.findOrCreate(existingUrl) must beSome
        val newUrl = "https://example.com/" + time
        val uriA = new URI(newUrl)
        val hierarchicalPartA = uriA.getRawAuthority + uriA.getRawPath
        Uri.findByHierarchicalPart(hierarchicalPartA).isEmpty must beTrue
	      val json = "["+
	        	"{\"url\":\""+newUrl+"\",\"time\":"+time+",\"clean\":0},"+
	        	"{\"url\":\""+invalidUrl+"\",\"time\":"+time+",\"clean\":0},"+
	        	"{\"url\":\""+existingUrl+"\",\"time\":"+time+",\"clean\":"+cleanTime+"}"+
	        "]"
	      Blacklist.importBlacklist(json, Source.NSF)
	      val foundA = Uri.findByHierarchicalPart(hierarchicalPartA)
	      foundA.nonEmpty must beTrue
	      val filteredA = foundA.filter(_.uri.equalsIgnoreCase(uriA.toString))
	      filteredA.nonEmpty must beTrue
	      filteredA.filter(_.isBlacklisted).nonEmpty must beTrue
	      filteredA.filter(_.isBlacklisted).map { uri =>
        	BlacklistEvent.findBlacklistedByUri(uri.id, Some(Source.NSF)).nonEmpty must beTrue
        }
        val uriB = new URI("http://"+existingUrl)
        val hierarchicalPartB = uriB.getRawAuthority + uriB.getRawPath
	      val foundB = Uri.findByHierarchicalPart(hierarchicalPartB)
	      foundB.nonEmpty must beTrue
	      val filteredB = foundB.filter(_.uri.equalsIgnoreCase(uriB.toString))
	      filteredB.nonEmpty must beTrue
	      filteredB.filter(_.isBlacklisted).map { uri =>
        	BlacklistEvent.findBlacklistedByUri(uri.id, Some(Source.NSF)).isEmpty must beTrue
        }
      }      
    }
    
  }

}
