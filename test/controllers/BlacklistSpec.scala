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
      val json = "[{\"url\":\"www.example.com/\",\"time\":1384958939}, {\"url\":\"example.com/\",\"time\":1337958939}]"
      Blacklist.importBlacklist(json, "goog")	//TODO WTSN-11 VERIFY
    }
    
    "import Google appeal results" in {
      //TODO WTSN-11
    }
    
    "import NSF blacklist" in {
      //TODO WTSN-11
    }
    
  }

}