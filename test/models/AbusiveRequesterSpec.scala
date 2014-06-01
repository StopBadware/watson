package models

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class AbusiveRequesterSpec extends Specification {
  
  private def testEmail = "test"+System.nanoTime.toHexString+"@example.com"
  
  private val testUser = {
    running(FakeApplication()) {
	    val testEmail = sys.env("TEST_EMAIL")
		  if (User.findByEmail(testEmail).isEmpty) {
		    User.create(testEmail.split("@").head, testEmail)
		  }
	    val user = User.findByEmail(testEmail).get
	    user.addRole(models.enums.Role.VERIFIER)
	    user.id
    }
  }
  
  "AbusiveRequester" should {
    
    "flag an email address as abusive" in {
      running(FakeApplication()) {
        val email = testEmail
        AbusiveRequester.flag(email, testUser) must beTrue
        AbusiveRequester.isFlagged(email) must beTrue
      }
    }
    
    "unflag an email address as abusive" in {
      running(FakeApplication()) {
        val email = testEmail
        AbusiveRequester.flag(email, testUser) must beTrue
        AbusiveRequester.unFlag(email, testUser) must beTrue
        AbusiveRequester.isFlagged(email) must beFalse
      }
    }
    
    "check if an email address is flagged" in {
      running(FakeApplication()) {
        val email = testEmail
        AbusiveRequester.isFlagged(email) must beFalse
        AbusiveRequester.flag(email, testUser) must beTrue
        AbusiveRequester.isFlagged(email) must beTrue
      }
    }
    
  }

}