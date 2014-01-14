package models

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class ReviewRequestSpec extends Specification {
  
  private def validUri: Uri = Uri.findOrCreate(UriSpec.validUri).get
  private def request: ReviewRequest = {
    val uri = validUri
    ReviewRequest.create(uri.id, "sylvanas@example.com")
    ReviewRequest.findByUri(uri.id).head
  }
  
  "ReviewRequest" should {
    
    "create a review request" in {
      running(FakeApplication()) {
        val uri = validUri
        ReviewRequest.findByUri(uri.id).isEmpty must beTrue
        ReviewRequest.create(uri.id, "thrall@example.com", Some(0L), Some("For the Horde!")) must beTrue
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