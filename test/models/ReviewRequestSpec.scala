package models

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._
import models.enums.{ClosedReason, Source}
import controllers.PostgreSql

@RunWith(classOf[JUnitRunner])
class ReviewRequestSpec extends Specification {
  
  private val numInBulk = 5
  private val email = "thrall@example.com"
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
        ReviewRequest.create(uri.id, email, Some(4294967295L), Some("For the Horde!")) must beTrue
        ReviewRequest.findByUri(uri.id).nonEmpty must beTrue
      }
    }
    
    "create a review after opening a review request" in {
      running(FakeApplication()) {
        val rr = request
        Review.findByUri(rr.uriId).map(_.isOpen) must not beEmpty
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
        rr.close(ClosedReason.ADMINISTRATIVE, Some(System.currentTimeMillis / 1000)) must beTrue
        ReviewRequest.find(rr.id).get.open must beFalse
      }
    }
    
    "reopen a review request" in {
      running(FakeApplication()) {
        val rr = request
        rr.open must beTrue	
        rr.close(ClosedReason.ADMINISTRATIVE, Some(System.currentTimeMillis / 1000)) must beTrue
        ReviewRequest.find(rr.id).get.open must beFalse
        rr.reopen() must beTrue
        ReviewRequest.find(rr.id).get.open must beTrue
      }
    }
    
    "update status but do NOT close review request when reason is REVIEWED_CLEAN" in {
      running(FakeApplication()) {
        val rr = request
        rr.open must beTrue	
        rr.closedReason must beEmpty
        rr.close(ClosedReason.REVIEWED_CLEAN) must beTrue
        val updated = ReviewRequest.find(rr.id).get
        updated.open must beTrue
        updated.closedReason must equalTo(Some(ClosedReason.REVIEWED_CLEAN))
        updated.close(ClosedReason.NO_PARTNERS_REPORTING) must beTrue
        val closed = ReviewRequest.find(rr.id).get
        closed.open must beFalse
        closed.closedReason must equalTo(Some(ClosedReason.REVIEWED_CLEAN))
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
    
    "find all open review requests" in {
      running(FakeApplication()) {
        val rr = request
        rr.open must beTrue
        val open = ReviewRequest.findOpen(None)
        open.nonEmpty must beTrue
        open.map(_.id).contains(rr.id) must beTrue
      }
    }
        
    "find review request by closed reason" in {
      running(FakeApplication()) {
        val rr = request
        rr.close(ClosedReason.ADMINISTRATIVE)
        val closed = ReviewRequest.findByClosedReason(Some(ClosedReason.ADMINISTRATIVE), PostgreSql.parseTimes(""))
        closed.nonEmpty must beTrue
        closed.map(_.id).contains(rr.id) must beTrue
      }
    }
    
  }

}