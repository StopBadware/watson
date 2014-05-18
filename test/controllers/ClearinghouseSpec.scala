package controllers

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._
import models.Uri
import models.enums.Source

@RunWith(classOf[JUnitRunner])
class ClearinghouseSpec extends Specification {
  
	private val source = Source.GOOG
  private def validUri(base: String): Uri = Uri.findOrCreate(base+".com/" + System.nanoTime.toHexString).get
  
  "Clearinghouse" should {
    
    "find all URIs for a host" in {
      running(FakeApplication()) {
        val time = (System.currentTimeMillis / 1000) - 1000
        val base = "chspec" + System.nanoTime.toHexString
        val uriA = validUri(base)
        val uriB = validUri(base)
        val uriC = validUri("www." + base)
        val uriD = validUri("blog." + base)
        val uriE = validUri("sub.www." + base)
        uriA.blacklist(source, time)
        uriB.blacklist(source, time, Some(System.currentTimeMillis / 1000))
        uriC.blacklist(source, time)
        uriD.blacklist(source, time)
        uriE.blacklist(source, time)
        val uriIds = List(uriA.id, uriB.id, uriC.id, uriD.id, uriE.id)
        
        val s1UriIds = Clearinghouse.findUrisWithSiblingsAndChildren(uriA.host, false).map(_.uriId)
        uriIds.map(s1UriIds.contains(_) must beTrue)
        
        val s2UriIds = Clearinghouse.findUrisWithSiblingsAndChildren(uriC.host, false).map(_.uriId)
        uriIds.map(s2UriIds.contains(_) must beTrue)
        
        val s3UriIds = Clearinghouse.findUrisWithSiblingsAndChildren(uriE.host, false).map(_.uriId)
        s3UriIds.contains(uriA.id) must beFalse
        s3UriIds.contains(uriD.id) must beFalse
        s3UriIds.contains(uriC.id) must beTrue
        s3UriIds.contains(uriE.id) must beTrue
      }
    }
    
    "find all currently blacklisted URIs for a host" in {
      running(FakeApplication()) {
        val time = (System.currentTimeMillis / 1000) - 1000
        val base = "chspec" + System.nanoTime.toHexString
        val uriA = validUri(base)
        val uriB = validUri(base)
        val uriC = validUri("www." + base)
        uriA.blacklist(source, time)
        uriB.blacklist(source, time, Some(System.currentTimeMillis / 1000))
        uriC.blacklist(source, time)
        val uriIds = List(uriA.id, uriB.id, uriC.id)
        
        val searchUriIds = Clearinghouse.findUrisWithSiblingsAndChildren(uriA.host, true).map(_.uriId)
        searchUriIds.contains(uriA.id) must beTrue
        searchUriIds.contains(uriB.id) must beFalse
        searchUriIds.contains(uriC.id) must beTrue
      }
    }
    
    "find blacklisted URI count and AS info for an IP" in {
      running(FakeApplication()) {
        false must beTrue	//TODO WTSN-50
      }
    }
    
    "find blacklisted URI and IP counts for an Autonomous System" in {
      running(FakeApplication()) {
        false must beTrue	//TODO WTSN-50
      }
    }
    
  }

}