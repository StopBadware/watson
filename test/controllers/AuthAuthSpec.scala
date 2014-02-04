package controllers

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class AuthAuthSpec extends Specification {
  
  val testEmail = sys.env("TEST_EMAIL")
  val testPassword = sys.env("TEST_PW")

  "AuthAuth" should {
    
    "create Stormpath account" in {
      running(FakeApplication()) {
      	//delete account at end of test
        true must beFalse
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
      	//use test account
        true must beFalse
      }
    }
    
  }
}