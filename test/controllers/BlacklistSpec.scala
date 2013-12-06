package controllers

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._
import models.{ReportedUri, Uri}

@RunWith(classOf[JUnitRunner])
class BlacklistSpec extends Specification {
  
  "Blacklist" should {
    
    "import differential blacklist" in {
      running(FakeApplication()) {
        val time = System.currentTimeMillis / 1000
        val existingUrl = "example.com"
        Uri.findOrCreate(new ReportedUri(existingUrl)) must beSome
        val newUrl = "https://example.com/" + time
        Uri.find(Hash.sha256(newUrl).get) must beNone
	      val json = "[{\"url\":\""+newUrl+"\",\"time\":"+time+"}, {\"url\":\""+existingUrl+"\",\"time\":"+time+"}]"
	      Blacklist.importBlacklist(json, "goog")	//TODO WTSN-11 VERIFY
	      val found = Uri.find(Hash.sha256(newUrl).get)
	      found must beSome
	      val uri = found.get
	      uri.isBlacklisted must beTrue
	      true must beFalse
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
