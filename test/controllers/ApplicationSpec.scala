package controllers

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class ApplicationSpec extends Specification {
  
  "Application" should {
    
    "send 404 on a bad request" in {
      running(FakeApplication()) {
        route(FakeRequest(GET, "/forthehorde")) must beNone        
      }
    }
    
    "redirect index to the welcome page" in {
      running(FakeApplication()) {
        val home = route(FakeRequest(GET, "/")).get
        
        status(home) must not equalTo(OK)
        status(home) must equalTo(SEE_OTHER)
      }
    }
    
    "render the welcome page" in {
      running(FakeApplication()) {
        val welcome = route(FakeRequest(GET, "/welcome")).get
        
        status(welcome) must equalTo(OK)
        contentType(welcome) must beSome.which(_ == "text/html")
        contentAsString(welcome) must contain ("Watson")
      }
    }
  }
}