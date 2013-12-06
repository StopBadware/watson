package controllers

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class BlacklistSpec extends Specification {
  
  "Blacklist" should {
    
    "import differential blacklist" in {
      running(FakeApplication()) {
        val time = +System.currentTimeMillis / 1000
	      val json = "[{\"url\":\"www.example.com/\",\"time\":"+time+"}, {\"url\":\"example.com/\",\"time\":"+time+"}]"
	      Blacklist.importBlacklist(json, "goog")	//TODO WTSN-11 VERIFY
	      true must be_==(false)
      }
    }
    
    "import Google appeal results" in {
      //TODO WTSN-11
      true must be_==(false)
    }
    
    "import NSF blacklist" in {
      //TODO WTSN-11
      true must be_==(false)
    }
    
  }

}
