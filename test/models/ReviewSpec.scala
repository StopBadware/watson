package models

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._
import models.enums.ReviewStatus

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

  "Review" should {
    
    "create a review" in {
      running(FakeApplication()) {
        val uri = validUri
        Review.create(uri.id) must beTrue
        Review.findByUri(uri.id).nonEmpty must beTrue
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
    
    "determines if trade is in a closed state or not" in {
      running(FakeApplication()) {
        val rev = createAndFind
        //TODO WTSN-31 
        true must beFalse		//DELME WTSN-31
      }
    }
    
    "add a tag" in {
      running(FakeApplication()) {
        val rev = createAndFind
        true must beFalse		//DELME WTSN-31
      }
    }
    
    "remove a tag" in {
      running(FakeApplication()) {
        val rev = createAndFind
        true must beFalse		//DELME WTSN-31
      }
    }
    
    "find a review" in {
      running(FakeApplication()) {
        val rev = createAndFind
        true must beFalse		//DELME WTSN-31
      }
    }
    
    "find reviews by tag name" in {
      running(FakeApplication()) {
        val rev = createAndFind
        true must beFalse		//DELME WTSN-31
      }
    }
    
    "find reviews by Uri" in {
      running(FakeApplication()) {
        val rev = createAndFind
        true must beFalse		//DELME WTSN-31
      }
    }
    
  }
  
}