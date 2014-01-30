package models

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._
import models.enums.{ClosedReason, ReviewStatus}

@RunWith(classOf[JUnitRunner])
class ReviewSpec extends Specification {
  
  private val reviewer = 1	//TODO WTSN-48 get from users table (create if needed)
  private val verifier = 1	//TODO WTSN-48 get from users table (create if needed)
  private def validUri: Uri = Uri.findOrCreate(UriSpec.validUri).get
  
  private def createAndFind: Review = {
    val uri = validUri
    Review.create(uri.id)
    Review.findByUri(uri.id).head
  }
  
  private def testTag: ReviewTag = {
    ReviewTag.create("TEST")
    ReviewTag.findByName("TEST").get
  } 

  "Review" should {
    
    "create a review" in {
      running(FakeApplication()) {
        val uri = validUri
        Review.create(uri.id) must beTrue
        Review.findByUri(uri.id).map(_.isOpen).size must equalTo(1)
      }
    }
    
    "not create a review if an open review already exists for a Uri" in {
      running(FakeApplication()) {
        val uri = validUri
        Review.create(uri.id) must beTrue
        Review.findByUri(uri.id).map(_.isOpen).size must equalTo(1)
        Review.create(uri.id) must beFalse
        Review.findByUri(uri.id).map(_.isOpen).size must equalTo(1)
      }
    }
    
    "delete a review" in {
      running(FakeApplication()) {
        val rev = createAndFind
        Review.find(rev.id) must beSome
        rev.delete() must beTrue
        Review.find(rev.id) must beNone
      }
    }
    
    "mark as reviewed" in {
      running(FakeApplication()) {
        val rev = createAndFind
        rev.status must equalTo(ReviewStatus.NEW)
        Thread.sleep(1500)	//make sure status update time will be different w/ second precision 
        rev.reviewed(ReviewStatus.BAD, reviewer)
        val findRev = Review.find(rev.id).get
        findRev.status must equalTo(ReviewStatus.PENDING)
        findRev.statusUpdatedAt must be_>(rev.statusUpdatedAt)
      }
    }
    
    "verify a review" in {
      running(FakeApplication()) {
        val rev = createAndFind
        rev.reviewed(ReviewStatus.BAD, reviewer) must beTrue
        rev.close(ReviewStatus.BAD, Some(verifier)) must beTrue
        Review.find(rev.id).get.status must equalTo(ReviewStatus.BAD)
      }
    }
    
    "close reviews that no longer have open review requests" in {
      running(FakeApplication()) {
        val revWithoutRequest = createAndFind
        ReviewRequest.findByUri(revWithoutRequest.uriId) must beEmpty
        val revWithClosedRequest = createAndFind
        ReviewRequest.create(revWithClosedRequest.uriId, "thrall@example.com")
        val request = ReviewRequest.findByUri(revWithClosedRequest.uriId)
        request.head.close(ClosedReason.NO_PARTNERS_REPORTING) must beTrue
        val revWithOpenRequest = createAndFind
        ReviewRequest.create(revWithOpenRequest.uriId, "sylvanas@example.com")
        val revWithOpenAndClosedRequest = createAndFind
        ReviewRequest.create(revWithOpenAndClosedRequest.uriId, "voljin@example.com")
        ReviewRequest.findByUri(revWithOpenAndClosedRequest.uriId).head.close(ClosedReason.NO_PARTNERS_REPORTING)
        ReviewRequest.create(revWithOpenAndClosedRequest.uriId, "baine@example.com")
        
        Review.closeAllWithoutOpenReviewRequests() must be_>(0)
        Review.find(revWithoutRequest.id).head.isOpen must beFalse
        Review.find(revWithClosedRequest.id).head.isOpen must beFalse
        Review.find(revWithOpenRequest.id).head.isOpen must beTrue
        Review.find(revWithOpenAndClosedRequest.id).head.isOpen must beTrue
      }
    }
    
    "reject a review" in {
      running(FakeApplication()) {
        val rev = createAndFind
        rev.reviewed(ReviewStatus.BAD, reviewer) must beTrue
        rev.reject(verifier, "REJECTED") must beTrue
        Review.find(rev.id).get.status must equalTo(ReviewStatus.REJECTED)
      }
    }
    
    "reopen a review" in {
      running(FakeApplication()) {
        val rev = createAndFind
        rev.close(ReviewStatus.CLOSED_WITHOUT_REVIEW) must beTrue
        Review.find(rev.id).get.reopen() must beTrue
        Review.find(rev.id).get.status must equalTo(ReviewStatus.REOPENED)
      }
    }
    
    "determines if review is in a closed state or not" in {
      running(FakeApplication()) {
        val rev = createAndFind
        rev.isOpen must beTrue
        rev.close(ReviewStatus.CLOSED_WITHOUT_REVIEW)
        Review.find(rev.id).get.isOpen must beFalse
      }
    }
    
    "add a tag" in {
      running(FakeApplication()) {
        val rev = createAndFind
        val tagId = testTag.id
        rev.addTag(tagId) must beTrue
        Review.find(rev.id).get.reviewTags.contains(tagId) must beTrue
      }
    }
    
    "remove a tag" in {
      running(FakeApplication()) {
        val rev = createAndFind
        val tagId = testTag.id
        val tagToRemoveId = {
          val name = "TEST"+System.currentTimeMillis.toHexString
			    ReviewTag.create(name)
			    ReviewTag.findByName(name).get.id
			  }
        rev.addTag(tagId)
        rev.addTag(tagToRemoveId)
        Review.find(rev.id).get.reviewTags.contains(tagToRemoveId) must beTrue
        rev.removeTag(tagToRemoveId)
        Review.find(rev.id).get.reviewTags.contains(tagToRemoveId) must beFalse
        Review.find(rev.id).get.reviewTags.contains(tagId) must beTrue
      }
    }
    
    "find a review" in {
      running(FakeApplication()) {
        val rev = createAndFind
        Review.find(rev.id) must beSome
      }
    }
    
    "find reviews by tag name" in {
      running(FakeApplication()) {
        val rev = createAndFind
        rev.addTag(testTag.id)
        Review.findByTag(testTag.id).contains(rev) must beTrue
      }
    }
    
    "find reviews by Uri" in {
      running(FakeApplication()) {
        val rev = createAndFind
        Review.findByUri(rev.uriId) must not beEmpty
      }
    }
    
  }
  
}