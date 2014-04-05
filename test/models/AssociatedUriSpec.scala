package models

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class AssociatedUriSpec extends Specification {
  
  private def testReview = Review.findOpenOrCreate(Uri.findOrCreate(UriSpec.validUri).get.id).get
  
  private def createAssocUri: AssociatedUri = {
    val rev = testReview
    AssociatedUri.create(rev.id, rev.uriId, None, None, None)
    AssociatedUri.findByReviewId(rev.id).head
  }
  
  "AssociatedUri" should {
    
    "create an AssociatedUri" in {
      running(FakeApplication()) {
        val rev = testReview
        AssociatedUri.create(rev.id, rev.uriId, None, None, None) must beTrue
      }
    }
    
    "delete an AssociatedUri" in {
      running(FakeApplication()) {
        val au = createAssocUri
        au.delete() must beTrue
        AssociatedUri.find(au.id) must beNone
      }
    }
    
    "update an Associated ri" in {
      running(FakeApplication()) {
        val au = createAssocUri
        au.resolved must beNone
        au.uriType must beNone
        au.intent must beNone
        val newType = Some("Payload")
        val newIntent = Some("Hacked")
        au.update(Some(true), newType, newIntent)
        val found = AssociatedUri.find(au.id).get
        found.resolved must equalTo(Some(true))
        found.uriType must equalTo(newType)
        found.intent must equalTo(newIntent)
      }
    }
    
    "find an AssociatedUri" in {
      running(FakeApplication()) {
        val au = createAssocUri
        AssociatedUri.find(au.id) must beSome
      }
    }
    
    "find AssociatedUris by ReviewId" in {
      running(FakeApplication()) {
        val au = createAssocUri
        AssociatedUri.findByReviewId(au.reviewId).nonEmpty must beTrue
      }
    }
    
    "find AssociatedUris by UriId" in {
      running(FakeApplication()) {
        val au = createAssocUri
        AssociatedUri.findByUriId(au.uriId).nonEmpty must beTrue
      }
    }
    
  }

}