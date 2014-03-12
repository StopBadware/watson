package models

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._
import models.enums._

@RunWith(classOf[JUnitRunner])
class ReviewSpec extends Specification {
  
  private val testUser = {
    running(FakeApplication()) {
	    val testEmail = sys.env("TEST_EMAIL")
		  if (User.findByEmail(testEmail).isEmpty) {
		    User.create(testEmail.split("@").head, testEmail)
		  }
	    val user = User.findByEmail(testEmail).get
	    user.addRole(Role.REVIEWER)
	    user.addRole(Role.VERIFIER)
	    user.id
    }
  }
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
        rev.reviewed(ReviewStatus.CLOSED_BAD, testUser)
        val findRev = Review.find(rev.id).get
        findRev.status must equalTo(ReviewStatus.PENDING_BAD)
        findRev.statusUpdatedAt must be_>(rev.statusUpdatedAt)
      }
    }
    
    "verify a review" in {
      running(FakeApplication()) {
        val rev = createAndFind
        rev.reviewed(ReviewStatus.CLOSED_BAD, testUser) must beTrue
        rev.verify(testUser, ReviewStatus.CLOSED_BAD) must beTrue
        Review.find(rev.id).get.status must equalTo(ReviewStatus.CLOSED_BAD)
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
        ReviewRequest.create(revWithOpenAndClosedRequest.uriId, "baine@example.com")
        ReviewRequest.findByUri(revWithOpenAndClosedRequest.uriId).head.close(ClosedReason.NO_PARTNERS_REPORTING)
        
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
        rev.reviewed(ReviewStatus.CLOSED_BAD, testUser) must beTrue
        rev.reject(testUser, "REJECTED") must beTrue
        Review.find(rev.id).get.status must equalTo(ReviewStatus.REJECTED)
      }
    }
    
    "reopen a review" in {
      running(FakeApplication()) {
        val rev = createAndFind
        rev.closeWithoutReview() must beTrue
        Review.find(rev.id).get.reopen() must beTrue
        Review.find(rev.id).get.status must equalTo(ReviewStatus.REOPENED)
      }
    }
    
    "determines if review is in a closed state or not" in {
      running(FakeApplication()) {
        val rev = createAndFind
        rev.isOpen must beTrue
        rev.closeWithoutReview()
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
        val testTagId = testTag.id
        rev.addTag(testTagId)
        Review.findByTag(testTagId).map(_.id).contains(rev.id) must beTrue
      }
    }
    
    "find reviews by Uri" in {
      running(FakeApplication()) {
        val rev = createAndFind
        Review.findByUri(rev.uriId) must not beEmpty
      }
    }
    
    "retrieve review summaries" in {
      running(FakeApplication()) {
        val rev = createAndFind
        val summaries = Review.summaries(new ReviewSummaryParams(None, None, None))
        summaries.nonEmpty must beTrue
        summaries.map(_.uri).contains(Uri.find(rev.uriId).get.uri) must beTrue
      }
    }
    
    "parse review summary params" in {
      running(FakeApplication()) {
        val allOpen = new ReviewSummaryParams(Some("all-open"), Some("any"), Some(""))
        allOpen.reviewStatus must equalTo(ReviewStatus.PENDING_BAD)
        allOpen.operator must equalTo("<=")
        allOpen.blacklistedBy must beNone
        allOpen.createdAt._1 must equalTo(new java.sql.Timestamp(0))
        
        val allClosed = new ReviewSummaryParams(Some("all-closed"), Some("any"), Some(""))
        allClosed.reviewStatus must equalTo(ReviewStatus.PENDING_BAD)
        allClosed.operator must equalTo(">")
        allClosed.blacklistedBy must beNone
        allClosed.createdAt._1 must equalTo(new java.sql.Timestamp(0))
        
        ReviewStatus.statuses.values.map { status =>
          val statusParams = new ReviewSummaryParams(Some(status.toString.toLowerCase), None, None)
          statusParams.reviewStatus must equalTo(status)
          statusParams.operator must equalTo("=")
          val statusWithSpacesParams = new ReviewSummaryParams(Some(status.toString.toLowerCase.replaceAll("_", " ")), None, None)
          statusWithSpacesParams.reviewStatus must equalTo(status)
          statusWithSpacesParams.operator must equalTo("=")
        }
        
        List(Source.GOOG, Source.NSF, Source.TTS).map { source =>
          val sourceParams = new ReviewSummaryParams(None, Some(source.abbr.toLowerCase), None)
          sourceParams.blacklistedBy must equalTo(Some(source))
        }
        
        val withDateRange = new ReviewSummaryParams(None, None, Some("01 Jan 2013 - 31 Dec 2013"))
        withDateRange.createdAt._1.toString must equalTo("2013-01-01 00:00:00.0")
        withDateRange.createdAt._2.toString must equalTo("2013-12-31 23:59:59.999")
        
        val withDate = new ReviewSummaryParams(None, None, Some("08 Jan 2011"))
        withDate.createdAt._1.toString must equalTo("2011-01-08 00:00:00.0")
        withDate.createdAt._2.toString must equalTo("2011-01-08 23:59:59.999")
      }
    }
    
    "find nearest open and pending reviews" in {
      running(FakeApplication()) {
        val prevRev = createAndFind
        val rev = createAndFind
        val nextRev = createAndFind
        
        val prevPen = createAndFind
        val pen = createAndFind
        val nextPen = createAndFind
        List(prevPen, pen, nextPen).foreach(_.reviewed(ReviewStatus.PENDING_BAD, testUser))
        
        val prevClosed = createAndFind
        val closed = createAndFind
        val nextClosed = createAndFind
        List(prevClosed, closed, nextClosed).foreach(_.closeWithoutReview())
        
        val revSiblings = Review.find(rev.id).get.siblings
        revSiblings("prev").get must equalTo(prevRev.id)
        revSiblings("next").get must equalTo(nextRev.id)
        
        val penSiblings = Review.find(pen.id).get.siblings
        penSiblings("prev").get must equalTo(prevPen.id)
        penSiblings("next").get must equalTo(nextPen.id)
        
        val closedSiblings = Review.find(closed.id).get.siblings
        closedSiblings("prev").get must equalTo(prevClosed.id)
        closedSiblings("next").get must equalTo(nextClosed.id)
      }
    }
    
    "get review details" in {
      running(FakeApplication()) {
        val uriId = validUri.id
        val email = "sylvanas@example.com"
    		BlacklistEvent.create(List(uriId), Source.GOOG, (System.currentTimeMillis / 1000) - 10000, None) must be_>(0)
    		ReviewRequest.create(uriId, email, Some(0), Some("FOR THE HORDE!")) must beTrue
    		BlacklistEvent.unBlacklist(uriId, Source.GOOG, (System.currentTimeMillis / 1000) - 1000)
    		ReviewRequest.findByUri(uriId).head.close(ClosedReason.NO_PARTNERS_REPORTING, None) must beTrue
    		
    		BlacklistEvent.create(List(uriId), Source.TTS, (System.currentTimeMillis / 1000) - 500, None) must be_>(0)
    		BlacklistEvent.create(List(uriId), Source.GOOG, System.currentTimeMillis / 1000, None) must be_>(0)
    		GoogleRescan.create(uriId, Some(validUri.id), "TEST", "TEST", System.currentTimeMillis / 1000) must beTrue
    		ReviewRequest.create(uriId, email, Some(0), Some("Lok'tar!")) must beTrue
    		
    		val reviews = Review.findByUri(uriId)
    		reviews.nonEmpty must beTrue
    		val review = reviews.filter(_.isOpen).head
    		val tag = {
		  		ReviewTag.create("TEST")
		  		ReviewTag.findByName("TEST").get
    		}
    		review.addTag(tag.id) must beTrue
    		val details = review.details
    		
    		details.uri.id must equalTo(uriId)
    		details.review.id must equalTo(review.id)
    		details.otherReviews.nonEmpty must beTrue
    		details.otherReviews.contains(review) must beFalse
    		details.blacklistEvents.nonEmpty must beTrue
    		details.googleRescans.nonEmpty must beTrue
    		details.googleRescans.contains(GoogleRescan.findByUri(uriId).head) must beTrue
    		details.reviewRequests.nonEmpty must beTrue
    		details.reviewRequests.filter(_.email.equalsIgnoreCase(email)).nonEmpty must beTrue
    		details.tags.nonEmpty must beTrue
    		details.tags.contains(tag.id) must beTrue
    		List("prev", "next").map(review.siblings.contains(_) must beTrue)
    		details.tags(tag.id) must equalTo(tag)
    		details.rescanUris.contains(GoogleRescan.findByUri(uriId).head.uriId) must beTrue
      }
    }
    
  }
  
}