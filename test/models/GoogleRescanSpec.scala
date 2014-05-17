package models

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class GoogleRescanSpec extends Specification {

  "GoogleRescan" should {
    
    "add a rescan" in {
      running(FakeApplication()) {
        val uriA = Uri.findOrCreate(UriSpec.validUri).get
        val uriB = Uri.findOrCreate(UriSpec.validUri).get
        GoogleRescan.create(uriA.id, None, "clean", "autoappeal", uriA.createdAt) must beTrue
        GoogleRescan.create(uriA.id, None, "clean", "autoappeal", uriA.createdAt) must beFalse
        GoogleRescan.create(uriA.id, Some(uriB.id), "bad", "autoappeal", uriA.createdAt) must beTrue
        GoogleRescan.create(uriA.id, Some(uriB.id), "bad", "autoappeal", uriA.createdAt) must beFalse
      }
      
    }
    
    "delete a rescan" in {
      running(FakeApplication()) {
        val uri = Uri.findOrCreate(UriSpec.validUri).get
      	GoogleRescan.create(uri.id, None, "clean", "autoappeal", uri.createdAt) must beTrue
      	val found = GoogleRescan.findByUri(uri.id)
      	found.nonEmpty must beTrue
      	found.filter(_.rescannedAt==uri.createdAt).head.delete()
      	GoogleRescan.findByUri(uri.id).filter(_.rescannedAt==uri.createdAt).isEmpty must beTrue
      }
    }
    
    "find rescans by URI" in {
      running(FakeApplication()) {
        val uri = Uri.findOrCreate(UriSpec.validUri).get
        GoogleRescan.findByUri(uri.id).isEmpty must beTrue
      	GoogleRescan.create(uri.id, None, "clean", "autoappeal", uri.createdAt)
      	GoogleRescan.findByUri(uri.id).nonEmpty must beTrue
      }
    }
    
    "get time of last rescan" in {
      running(FakeApplication()) {
        val uri = Uri.findOrCreate(UriSpec.validUri).get
      	GoogleRescan.create(uri.id, None, "clean", "autoappeal", uri.createdAt)
      	GoogleRescan.timeOfLast must be_>=(uri.createdAt)
      }
    }
    
  }
  
}