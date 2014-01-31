package models

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class UserSpec extends Specification {
  
  "User" should {
    
  	"create a new user" in {
  	  running(FakeApplication()) {
  	    true must beFalse		//DELME WTSN-48
  	  }
  	}
  	
  	"delete a user" in {
  	  running(FakeApplication()) {
  	    true must beFalse		//DELME WTSN-48
  	  }
  	}
  	
  	"add a role" in {
  	  running(FakeApplication()) {
  	    true must beFalse		//DELME WTSN-48
  	  }
  	}
  	
  	"remove a role" in {
  	  running(FakeApplication()) {
  	    true must beFalse		//DELME WTSN-48
  	  }
  	}
  	
  	"find a user" in {
  	  running(FakeApplication()) {
  	    true must beFalse		//DELME WTSN-48
  	  }
  	}
  	
  	"find a user by username/email" in {
  	  running(FakeApplication()) {
  	    true must beFalse		//DELME WTSN-48
  	  }
  	}
    
  }

}