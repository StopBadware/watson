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
    
    "add differential blacklist to queue" in {
      running(FakeApplication()) {
        val time = System.currentTimeMillis / 1000
        val urlA = "example.com"
        val urlB = "https://example.com/" + (time*1000)
	      val json = "[{\"url\":\""+urlB+"\",\"time\":"+time+"}, {\"url\":\""+urlA+"\",\"time\":"+time+"}]"
	      Blacklist.importBlacklist(json, Source.GOOG)
	      true must beFalse //TODO WTSN-39 verify import is in queue
      }
    }    
    
    "add new entries from differential blacklist" in {
      running(FakeApplication()) {
        val time = System.currentTimeMillis / 1000
        val existingUrl = "example.com"
        val existingUri = new ReportedUri(existingUrl)
        Uri.findOrCreate(existingUri) must beSome
        val newUrl = "https://example.com/" + (time*1000)
        val newUri = new ReportedUri(newUrl)
	      val uris = List(existingUri, newUri)
	      Blacklist.importDifferential(uris, Source.GOOG, time)
	      
	      val found = Uri.findByHierarchicalPart(newUri.hierarchicalPart)
	      found.nonEmpty must beTrue
	      val filtered = found.filter(_.uri.equals(newUri.uri.toString))
	      filtered.nonEmpty must beTrue
	      filtered.filter(_.isBlacklisted).nonEmpty must beTrue
	      filtered.filter(_.isBlacklisted).map { uri =>
        	BlacklistEvent.findBlacklistedByUri(uri.id, Some(Source.GOOG)).nonEmpty must beTrue
        }
      }
    }
    
    "update entries that fall off differential blacklist (imported in order)" in {
      running(FakeApplication()) {
        val timeA = (System.currentTimeMillis / 1000) - 42
	      val timeB = System.currentTimeMillis / 1000
	      
	      val uriA = new ReportedUri("example.com/" + timeA)
        val uriB = new ReportedUri("example.com/")
	      val urisA = List(uriA, uriB)
	      val urisB = List(uriB)
	      
	      Blacklist.importDifferential(urisA, Source.GOOG, timeA)
	      val foundA = Uri.findByHierarchicalPart(uriA.hierarchicalPart)
	      foundA.nonEmpty must beTrue
	      val filtered = foundA.filter(_.uri.equals(uriA.uri.toString))
	      filtered.nonEmpty must beTrue
	      filtered.filter(_.isBlacklisted).nonEmpty must beTrue
	      filtered.filter(_.isBlacklisted).map { uri =>
        	BlacklistEvent.findBlacklistedByUri(uri.id, Some(Source.GOOG)).nonEmpty must beTrue
        }
        
        Blacklist.importDifferential(urisB, Source.GOOG, timeB)
	      val foundB = Uri.findByHierarchicalPart(uriA.hierarchicalPart)
	      foundB.nonEmpty must beTrue
        foundB.filter(_.uri.equals(uriA.uri.toString)).nonEmpty must beTrue
	      foundB.filter(u => u.uri.equals(uriA.uri.toString) && u.isBlacklisted).isEmpty must beTrue
	      foundB.filter(_.isBlacklisted).map { uri =>
        	BlacklistEvent.findBlacklistedByUri(uri.id, Some(Source.GOOG)).nonEmpty must beTrue
        }
      }
    }
    
    "update entries that fall off differential blacklist (imported out of order)" in {
      running(FakeApplication()) {
        val timeA = (System.currentTimeMillis / 1000) - 1337
	      val timeB = System.currentTimeMillis / 1000
	      
	      val uriA = new ReportedUri("example.com/" + timeA)
        val uriB = new ReportedUri("example.com/")
	      val urisA = List(uriA, uriB)
	      val urisB = List(uriB)
	      
        Blacklist.importDifferential(urisB, Source.GOOG, timeB)
	      Blacklist.importDifferential(urisA, Source.GOOG, timeA)
	      
	      val foundA = Uri.findByHierarchicalPart(uriA.hierarchicalPart)
	      foundA.nonEmpty must beTrue
	      val filtered = foundA.filter(_.uri.equals(uriA.uri.toString))
	      filtered.nonEmpty must beTrue
	      filtered.filter(_.isBlacklisted).nonEmpty must beTrue
	      filtered.filter(_.isBlacklisted).map { uri =>
        	BlacklistEvent.findBlacklistedByUri(uri.id, Some(Source.GOOG)).nonEmpty must beTrue
        }
	      
	      val foundB = Uri.findByHierarchicalPart(uriA.hierarchicalPart)
	      foundB.nonEmpty must beTrue
        foundB.filter(_.uri.equals(uriA.uri.toString)).nonEmpty must beTrue
	      foundB.filter(u => u.uri.equals(uriA.uri.toString) && u.isBlacklisted).isEmpty must beTrue
	      foundB.filter(_.isBlacklisted).map { uri =>
        	BlacklistEvent.findBlacklistedByUri(uri.id, Some(Source.GOOG)).nonEmpty must beTrue
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
	        "{\"url\":\""+cleanUrl+"\",\"status\":\"clean\",\"time\":"+time+",\"source\":\"autoappeal\"}]"
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
