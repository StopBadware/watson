package controllers

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._
import java.net.URI
import models.{ReportedUri, Source, Uri}

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
	      val timeB = System.currentTimeMillis / 1000
	      val jsonB = "[{\"url\":\"example.com\",\"time\":"+timeB+"}]"
	      Blacklist.importBlacklist(jsonB, Source.GOOG)
	      val foundB = Uri.findByHierarchicalPart(hierarchicalPart)
	      foundB.nonEmpty must beTrue
        foundB.filter(_.uri.equals(uri.toString)).nonEmpty must beTrue
	      foundB.filter(u => u.uri.equals(uri.toString) && u.isBlacklisted).isEmpty must beTrue
      }
    }    
    
    "import Google appeal results" in {
      //TODO WTSN-11
      true must beFalse
    }
    
    "import NSF blacklist" in {
      //TODO WTSN-11
      true must beFalse
    }
    
  }

}
