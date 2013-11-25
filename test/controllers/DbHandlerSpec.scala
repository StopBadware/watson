package controllers

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._
import models.ReportedUri

@RunWith(classOf[JUnitRunner])
class DbHandlerSpec extends Specification {
  
  "DbHandler" should {
    
    val testUri = "https://example.com/some/path?q=query&a=another#fragment"
    val reported = new ReportedUri(testUri)
    val uriDoc = DbHandler.findOrCreate(reported)
   
    "find an existing document matching a reported URI" in {
      val doc = DbHandler.findOrCreate(reported)
      doc.isDefined must beTrue
//      doc.get must beAnInstanceOf[] //TODO WTSN-11
    }
    
    "create a new document matching a reported URI" in {
      val nowUri = new ReportedUri("http://example.com/test?now="+System.currentTimeMillis)
      val doc = DbHandler.findOrCreate(nowUri)
      doc.isDefined must beTrue
//      doc.get must beAnInstanceOf[] //TODO WTSN-11
    }
    
  }
  
  "Hash" should {
    
    "hash a string using SHA2-256" in {
      val string = "The quick brown fox jumps over the lazy dog"
      val hashed = "d7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb762d02d0bf37c9e592"
      Hash.sha256(string).get must equalTo(hashed)
    }
    
  }

}