package controllers

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class MailerSpec extends Specification {
  
  private val email = "test@stopbadware.org"
  private val uri = "http://example.com/"
  
  "Mailer" should {
    
    "send notification email for review of uri no longer blacklisted" in {
      running(FakeApplication()) {
        Mailer.sendNoLongerBlacklisted(email, uri).apply() must beTrue
      }
    }
    
    "send notification email after closing review request bad" in {
      running(FakeApplication()) {
        Mailer.sendReviewClosedBad(email, uri, "Your site needs more cowbell").apply() must beTrue
      }
    }
    
    "send notification email after closing TTS review request clean" in {
      running(FakeApplication()) {
        Mailer.sendReviewClosedCleanTts(email, uri).apply() must beTrue
      }
    }
    
    "send notification email after receiving review request" in {
      running(FakeApplication()) {
        Mailer.sendReviewRequestReceived(email, uri).apply() must beTrue
      }
    }
    
  }

}