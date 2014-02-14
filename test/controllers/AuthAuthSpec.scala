package controllers

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._
import models.User

@RunWith(classOf[JUnitRunner])
class AuthAuthSpec extends Specification {
  
	private val invalidEmail = "test"+System.nanoTime.toHexString+"@example.com"
  private val testEmail = sys.env("TEST_EMAIL")
  private val testPw = sys.env("TEST_PW")
  running(FakeApplication()) {
	  if (User.findByEmail(testEmail).isEmpty) {
	    User.create(testEmail.split("@").head, testEmail)
	  }
  }

  "AuthAuth" should {
    
    "create Stormpath account" in {
      running(FakeApplication()) {
        val validEmail = "test"+System.nanoTime.toHexString+"@stopbadware.org"
        AuthAuth.create(validEmail, testPw) must beTrue
        AuthAuth.create(validEmail, "") must beFalse
        AuthAuth.create(invalidEmail, testPw) must beFalse
        AuthAuth.delete(validEmail) must beTrue
      }
    }
    
    "authenticate an account" in {
      running(FakeApplication()) {
        AuthAuth.authenticate(testEmail, testPw) must beSome
        AuthAuth.authenticate(testEmail, "") must beNone
        AuthAuth.authenticate("", testPw) must beNone
        AuthAuth.authenticate("", "") must beNone
      }
    }
    
    "enable/disable account" in {
      running(FakeApplication()) {
        AuthAuth.enable(testEmail) must beTrue
        AuthAuth.authenticate(testEmail, testPw) must beSome
        AuthAuth.disable(testEmail) must beTrue
        AuthAuth.authenticate(testEmail, testPw) must beNone
        AuthAuth.enable(testEmail) must beTrue
        AuthAuth.authenticate(testEmail, testPw) must beSome
      }
    }
    
    "send password reset email" in {
      running(FakeApplication()) {
        AuthAuth.sendResetMail(invalidEmail) must beFalse
        AuthAuth.sendResetMail(testEmail) must beTrue
      }
    }
    
  }
}