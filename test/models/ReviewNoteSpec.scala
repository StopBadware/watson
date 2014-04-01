package models

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._
import models.enums._

@RunWith(classOf[JUnitRunner])
class ReviewNoteSpec extends Specification {
  
  private val testUserId = {
    running(FakeApplication()) {
	    val testEmail = sys.env("TEST_EMAIL")
		  if (User.findByEmail(testEmail).isEmpty) {
		    User.create(testEmail.split("@").head, testEmail)
		  }
	    val user = User.findByEmail(testEmail).get
	    user.id
    }
  }
  
  private val testReviewId = {
    running(FakeApplication()) {
    	Review.findOpenOrCreate(Uri.findOrCreate(UriSpec.validUri).get.id).get.id
    }
  }
  
  private def testStr: String = System.currentTimeMillis.toHexString + System.nanoTime.toHexString
  
  "ReviewNote" should {
    
    "create a review note" in {
      running(FakeApplication()) {
        ReviewNote.create(testReviewId, testUserId, testStr) must beTrue
      }
    }
    
    "find a review note" in {
      running(FakeApplication()) {
        val note = testStr
        ReviewNote.create(testReviewId, testUserId, note) must beTrue
        val findByAuthor = ReviewNote.findByAuthor(testUserId)
        findByAuthor.nonEmpty must beTrue
        val rn = findByAuthor.filter(_.note.equalsIgnoreCase(note)).head
        ReviewNote.find(rn.id) must beSome
      }
    }
    
    "delete a review note" in {
      running(FakeApplication()) {
        val note = testStr
        ReviewNote.create(testReviewId, testUserId, note) must beTrue
        val rn = ReviewNote.findByAuthor(testUserId).filter(_.note.equalsIgnoreCase(note)).head
        rn.delete() must beTrue
        ReviewNote.findByAuthor(testUserId).filter(_.note.equalsIgnoreCase(note)).isEmpty must beTrue
      }
    }
    
  }

}