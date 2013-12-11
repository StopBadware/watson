package controllers

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._
import java.net.URI
import models._

@RunWith(classOf[JUnitRunner])
class BlacklistSpec extends Specification {
  
  "Blacklist" should {
    
    "add new entries from differential blacklist" in {
      running(FakeApplication()) {
        val time = System.currentTimeMillis / 1000
        val existingUrl = "example.com"
        Uri.findOrCreate(new ReportedUri(existingUrl)) must beSome
        val newUrl = "https://example.com/" + time
        val uri = new URI(newUrl)
        val hierarchicalPart = uri.getRawAuthority + uri.getRawPath
        Uri.findByHierarchicalPart(hierarchicalPart).isEmpty must beTrue
	      val json = "[{\"url\":\""+newUrl+"\",\"time\":"+time+"}, {\"url\":\""+existingUrl+"\",\"time\":"+time+"}]"
	      Blacklist.importBlacklist(json, Source.GOOG)
	      val found = Uri.findByHierarchicalPart(hierarchicalPart)
	      found.nonEmpty must beTrue
	      val filtered = found.filter(_.uri.equals(uri.toString))
	      filtered.nonEmpty must beTrue
	      filtered.filter(_.isBlacklisted).nonEmpty must beTrue
	      filtered.filter(_.isBlacklisted).map { uri =>
        	BlacklistEvent.findBlacklistedByUri(uri.id, Some(Source.GOOG)).nonEmpty must beTrue
        }
      }
    }
    
    "update entries that fall off differential blacklist" in {
      running(FakeApplication()) {
        val timeA = (System.currentTimeMillis / 1000) - 1337
        val url = "example.com/" + timeA
        val uri = new URI("http://"+url)
        val hierarchicalPart = uri.getRawAuthority + uri.getRawPath
        Uri.findByHierarchicalPart(hierarchicalPart).isEmpty must beTrue
	      val jsonA = "[{\"url\":\""+url+"\",\"time\":"+timeA+"}, {\"url\":\"example.com\",\"time\":"+timeA+"}]"
	      Blacklist.importBlacklist(jsonA, Source.GOOG)
	      val foundA = Uri.findByHierarchicalPart(hierarchicalPart)
	      foundA.nonEmpty must beTrue
	      val filtered = foundA.filter(_.uri.equals(uri.toString))
	      filtered.nonEmpty must beTrue
	      filtered.filter(_.isBlacklisted).nonEmpty must beTrue
	      filtered.filter(_.isBlacklisted).map { uri =>
        	BlacklistEvent.findBlacklistedByUri(uri.id, Some(Source.GOOG)).nonEmpty must beTrue
        }
	      val timeB = System.currentTimeMillis / 1000
	      val jsonB = "[{\"url\":\"example.com\",\"time\":"+timeB+"}]"
	      Blacklist.importBlacklist(jsonB, Source.GOOG)
	      val foundB = Uri.findByHierarchicalPart(hierarchicalPart)
	      foundB.nonEmpty must beTrue
        foundB.filter(_.uri.equals(uri.toString)).nonEmpty must beTrue
	      foundB.filter(u => u.uri.equals(uri.toString) && u.isBlacklisted).isEmpty must beTrue
	      foundB.filter(_.isBlacklisted).map { uri =>
        	BlacklistEvent.findBlacklistedByUri(uri.id, Some(Source.GOOG)).nonEmpty must beTrue
        }
      }
    }    
    
    "import Google appeal results" in {
      running(FakeApplication()) {
	      val badUrl = "example.com/"
	      val badLink = "http://" + badUrl + (System.currentTimeMillis / 1000)
	      val cleanUrl = "cleanurl.com/"
	      val json = "[{\"url\":\""+badUrl+"\",\"status\":\"bad\",\"time\":1384862400,\"source\":\"autoappeal\",\"link\":\""+badLink+"\"},"+
	        "{\"url\":\""+cleanUrl+"\",\"status\":\"clean\",\"time\":1384905600,\"source\":\"autoappeal\"}]"
	      Blacklist.importGoogleAppeals(json)
	      val bad = Uri.find(new ReportedUri(badUrl).sha256)
	      bad must beSome
	      val link = Uri.find(new ReportedUri(badLink).sha256)
	      link must beSome
	      val clean = Uri.find(new ReportedUri(cleanUrl).sha256)
	      clean must beSome
	      true must beFalse		//TODO WTSN-11 verify rescans added
      }
    }
    
    "import NSF blacklist" in {
      running(FakeApplication()) {
        val time = System.currentTimeMillis / 1000 - 47
        val cleanTime = System.currentTimeMillis / 1000
        val existingUrl = "example.com"
        Uri.findOrCreate(new ReportedUri(existingUrl)) must beSome
        val newUrl = "https://example.com/" + time
        val uriA = new URI(newUrl)
        val hierarchicalPartA = uriA.getRawAuthority + uriA.getRawPath
        Uri.findByHierarchicalPart(hierarchicalPartA).isEmpty must beTrue
	      val json = "["+
	        	"{\"url\":\""+newUrl+"\",\"time\":"+time+",\"clean\":0},"+
	        	"{\"url\":\""+existingUrl+"\",\"time\":"+time+",\"clean\":"+cleanTime+"}"+
	        "]"
	      Blacklist.importBlacklist(json, Source.NSF)
	      val foundA = Uri.findByHierarchicalPart(hierarchicalPartA)
	      foundA.nonEmpty must beTrue
	      val filteredA = foundA.filter(_.uri.equals(uriA.toString))
	      filteredA.nonEmpty must beTrue
	      filteredA.filter(_.isBlacklisted).nonEmpty must beTrue
	      filteredA.filter(_.isBlacklisted).map { uri =>
        	BlacklistEvent.findBlacklistedByUri(uri.id, Some(Source.NSF)).nonEmpty must beTrue
        }
        val uriB = new URI("http://"+existingUrl)
        val hierarchicalPartB = uriB.getRawAuthority + uriB.getRawPath
	      val foundB = Uri.findByHierarchicalPart(hierarchicalPartB)
	      foundB.nonEmpty must beTrue
	      val filteredB = foundB.filter(_.uri.equals(uriB.toString))
	      filteredB.nonEmpty must beTrue
	      filteredB.filter(_.isBlacklisted).map { uri =>
        	BlacklistEvent.findBlacklistedByUri(uri.id, Some(Source.NSF)).isEmpty must beTrue
        }        
      }      
    }
    
  }

}
