package models

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._
import controllers.Hash

@RunWith(classOf[JUnitRunner])
class ReviewCodeSpec extends Specification {
  
  private def testReviewId: Int = Review.findOpenOrCreate(Uri.findOrCreate(UriSpec.validUri).get.id).get.id
  
  private def testHash: String = Hash.sha256(System.nanoTime.toHexString).get
  
  "ReviewCode" should {
    
    "create a review code" in {
      running(FakeApplication()) {
        ReviewCode.create(testReviewId, None, None) must beTrue
      }
    }
    
    "update a review code" in {
      running(FakeApplication()) {
        val sha256 = Some(testHash)
        ReviewCode.create(testReviewId, None, sha256)
        val rc = ReviewCode.findByExecutable(sha256.get).head
        rc.badCode must beNone
        rc.update(sha256, None)
        ReviewCode.find(rc.id).get.badCode must equalTo(sha256)
      }
    }
    
    "create or update a review code" in {
      running(FakeApplication()) {
        val sha256 = Some(testHash)
        val revId = testReviewId
        ReviewCode.create(revId, None, sha256)
        val rc = ReviewCode.findByExecutable(sha256.get).head
        rc.badCode must beNone
        ReviewCode.createOrUpdate(revId, sha256, None) must beTrue
        ReviewCode.find(rc.id).get.badCode must equalTo(sha256)
      }
    }
    
    "delete a review code" in {
      running(FakeApplication()) {
        val sha256 = testHash
        ReviewCode.create(testReviewId, None, Some(sha256))
        val rc = ReviewCode.findByExecutable(sha256).head
        rc.delete() must beTrue
        ReviewCode.findByExecutable(sha256).isEmpty must beTrue
      }
    }
    
    "find a review code" in {
      running(FakeApplication()) {
        val sha256 = testHash
        ReviewCode.create(testReviewId, None, Some(sha256))
        val id = ReviewCode.findByExecutable(sha256).head.id
        ReviewCode.find(id) must beSome
      }
    }
    
    "find review codes with same executable" in {
      running(FakeApplication()) {
        val sha256 = testHash
        ReviewCode.create(testReviewId, None, Some(sha256))
        ReviewCode.findByExecutable(sha256).nonEmpty must beTrue
      }
    }
    
    "find review code by review" in {
      running(FakeApplication()) {
        val revId = testReviewId
        ReviewCode.create(revId, None, None)
        ReviewCode.findByReview(revId) must beSome
      }
    }
    
  }

}