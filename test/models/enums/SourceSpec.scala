package models.enums

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class SourceSpec extends Specification {
  
  private val sbw = "SBW"
  
  "Source" should {
    
    "match string to source" in {
      Source.withAbbr("") must beNone
      Source.withAbbr(sbw.toUpperCase) must beSome
      Source.withAbbr(sbw.toLowerCase) must beSome
      val source = Source.withAbbr(sbw)
      source must beSome
      source.get must not equalTo(Source.SBWCR)
      source.get must equalTo(Source.SBW)
      Source.SBW must not equalTo(Source.SBWCR)
      Source.SBW must equalTo(Source.SBW)
    }
    
    "get short name of source" in {
      Source.SBW.toString must equalTo(sbw)
      Source.SBW.abbr must equalTo(sbw)
    }  
    
    "get full name of source" in {
      running(FakeApplication()) {
      	Source.SBW.fullName.isEmpty must beFalse
      }
    }      
    
  }

}