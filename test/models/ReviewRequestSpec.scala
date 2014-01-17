package models

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._
import models.enums.Source

@RunWith(classOf[JUnitRunner])
class ReviewRequestSpec extends Specification {
  
  private val numInBulk = 5
  private val email = "test@stopbadware.org"
  private def validUri: Uri = Uri.findOrCreate(UriSpec.validUri).get
  private def request: ReviewRequest = {
    val uri = validUri
    ReviewRequest.create(uri.id, email)
    ReviewRequest.findByUri(uri.id).head
  }
  
  "ReviewRequest" should {
    
    "create a review request" in {
      running(FakeApplication()) {
        val uri = validUri
        ReviewRequest.findByUri(uri.id).isEmpty must beTrue
        ReviewRequest.create(uri.id, email, Some(0L), Some("For the Horde!")) must beTrue
        ReviewRequest.findByUri(uri.id).nonEmpty must beTrue
      }
    }
    
    "not create a review request with invalid email" in {
      running(FakeApplication()) {
        val uri = validUri
        ReviewRequest.create(uri.id, "") must beFalse
        ReviewRequest.create(uri.id, "jaina") must beFalse
        ReviewRequest.create(uri.id, "example.com") must beFalse
        ReviewRequest.create(uri.id, "@example.com") must beFalse
        ReviewRequest.create(uri.id, "jaina@example") must beFalse
      }
    }    
    
    "close a review request" in {
      running(FakeApplication()) {
        val rr = request
        rr.open must beTrue	
        rr.close(Some(System.currentTimeMillis / 1000))
        ReviewRequest.find(rr.id).get.open must beFalse
      }
    }
    
    "close all review requests for uris no longer blacklisted" in {
      running(FakeApplication()) {
        val now = System.currentTimeMillis / 1000
        val sourceA = Source.SBW
        val sourceB = Source.GOOG
        val overlap = validUri.id
        val shouldClose = {
          val uris = (1 to numInBulk).foldLeft(List.empty[Int])((list, _) => list :+ validUri.id)
          BlacklistEvent.create(uris, sourceA, now, Some(now))
          BlacklistEvent.create(uris :+ overlap, sourceB, now, Some(now))
          uris.foreach(ReviewRequest.create(_, email))
          uris.map(ReviewRequest.findByUri(_))
        }.flatten
        val shouldNotClose = {
          val uris = (1 to numInBulk).foldLeft(List.empty[Int])((list, _) => list :+ validUri.id) :+ overlap
          BlacklistEvent.create(uris, sourceA, now, None)
          BlacklistEvent.create(uris, sourceB, now, None)
          uris.foreach(ReviewRequest.create(_, email))
          uris.map(ReviewRequest.findByUri(_))
        }.flatten
        ReviewRequest.closeNoLongerBlacklisted() must be_>=(shouldClose.size)
        shouldClose.map(_.id).map(ReviewRequest.find(_).get.open must beFalse)
        shouldNotClose.map(_.id).map(ReviewRequest.find(_).get.open must beTrue)
      }
    }
    
    "send notification email after closing review request" in {
      running(FakeApplication()) {
        true must beFalse	//TODO WTSN-30
      }
    }
    
    "delete a review request" in {
      running(FakeApplication()) {
        val rr = request
        ReviewRequest.find(rr.id) must beSome
        rr.delete()
        ReviewRequest.find(rr.id) must beNone
      }
    }
    
    "find a review request" in {
      running(FakeApplication()) {
        ReviewRequest.find(request.id) must beSome
      }
    }
    
    "find review request by uri" in {
      running(FakeApplication()) {
        ReviewRequest.findByUri(request.uriId).nonEmpty must beTrue
      }
    }
    
  }

}