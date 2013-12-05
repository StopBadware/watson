package models

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
      source.get must not be equalTo(Source.SBWCR)
      source.get must be equalTo(Source.SBW)
      Source.SBW must not be equalTo(Source.SBWCR)
      Source.SBW must be equalTo(Source.SBW)
    }
    
    "get short name of source" in {
      Source.SBW.toString must be equalTo(sbw)
      Source.SBW.abbr must be equalTo(sbw)
    }  
    
    "get full name of source" in {
      running(FakeApplication()) {
      	Source.SBW.fullName.isEmpty must be equalTo(false)
      }
    }      
    
  }

}