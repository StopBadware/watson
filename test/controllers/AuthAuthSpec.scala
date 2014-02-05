package controllers

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class AuthAuthSpec extends Specification {
  
  val testEmail = sys.env("TEST_EMAIL")
  val testPw = sys.env("TEST_PW")

  "AuthAuth" should {
    
    "create Stormpath account" in {
      running(FakeApplication()) {
        val validEmail = "test"+System.nanoTime.toHexString+"@stopbadware.org"
        val invalidEmail = "test"+System.nanoTime.toHexString+"@example.com"
        AuthAuth.create(validEmail, testPw) must beTrue
        AuthAuth.create(validEmail, "") must beFalse
        AuthAuth.create(invalidEmail, testPw) must beFalse
        AuthAuth.delete(validEmail) must beTrue
      }
    }
    
    "authenticate an account" in {
      running(FakeApplication()) {
        //use test account
        //return Some(User) on success
        //return None on failure
        true must beFalse
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
    
  }
}