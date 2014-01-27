package models

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._
import models.enums.ReviewStatus

@RunWith(classOf[JUnitRunner])
class ReviewSpec extends Specification {
  
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
//        rev.delete() must beTrue
        Review.find(rev.id) must beNone
      }
    }
    
    "mark as reviewed" in {
      running(FakeApplication()) {
        val rev = createAndFind
        rev.status must equalTo(ReviewStatus.NEW)
        rev.reviewed(ReviewStatus.BAD, 0)
        val findRev = Review.find(rev.id).get
        findRev.status must equalTo(ReviewStatus.PENDING)
        findRev.statusUpdatedAt must be_>(rev.statusUpdatedAt)
      }
    }
    
    "verify a review" in {
      running(FakeApplication()) {
        val rev = createAndFind
        rev.reviewed(ReviewStatus.BAD, 0)
        rev.status must equalTo(ReviewStatus.PENDING)
        rev.close(ReviewStatus.BAD, Some(0)) must beTrue
        val findRev = Review.find(rev.id).get
        findRev.status must equalTo(ReviewStatus.BAD)
        findRev.statusUpdatedAt must be_>(rev.statusUpdatedAt)
      }
    }
    
    "reject a review" in {
      running(FakeApplication()) {
        val rev = createAndFind
        rev.reviewed(ReviewStatus.BAD, 0)
        rev.status must equalTo(ReviewStatus.PENDING)
        rev.reject(0, "REJECTED")
        val findRev = Review.find(rev.id).get
        findRev.status must equalTo(ReviewStatus.REJECTED)
        findRev.statusUpdatedAt must be_>(rev.statusUpdatedAt)
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