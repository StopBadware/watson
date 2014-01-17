package controllers

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class MailerSpec extends Specification {
  
  "Mailer" should {
    
    "send notification email for review of uri no longer blacklisted" in {
      running(FakeApplication()) {
        true must beFalse	//TODO WTSN-30
      }
    }
    
    "send notification email after closing review request bad" in {
      running(FakeApplication()) {
        true must beFalse	//TODO WTSN-30
      }
    }
    
    "send notification email after closing TTS review request clean" in {
      running(FakeApplication()) {
        true must beFalse	//TODO WTSN-30
      }
    }
    
    "send an email" in {
      running(FakeApplication()) {
        true must beFalse	//TODO WTSN-30
      }
    }
    
  }

}