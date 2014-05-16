package models

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._
import models.enums.Role

@RunWith(classOf[JUnitRunner])
class UserSpec extends Specification {
  
  private def username: String = "test"+System.nanoTime.toHexString
  private def createAndGetUser: User = {
    val uname = username
    User.create(uname, uname+"@stopbadware.org")
    User.findByUsername(uname).get
  }
  
  "User" should {
    
  	"create a new user" in {
  	  running(FakeApplication()) {
  	    val uname = username
  	    User.create(uname, uname+"@stopbadware.org") must beTrue
  	    User.findByUsername(uname) must beSome
  	  }
  	}
  	
  	"do NOT create a new user if email address not stopbadware.org" in {
  	  running(FakeApplication()) {
  	    val uname = username
  	    User.create(uname, uname+"@example.com") must beFalse
  	    User.findByUsername(uname) must beNone
  	  }
  	}
  	
  	"delete a user" in {
  	  running(FakeApplication()) {
  	    val user = createAndGetUser
  	    User.findByUsername(user.username) must beSome
  	    user.delete()
  	    User.findByUsername(user.username) must beNone
  	  }
  	}
  	
  	"add a role" in {
  	  running(FakeApplication()) {
  	    val user = createAndGetUser
  	    user.roles.contains(Role.REVIEWER) must beFalse
  	    user.addRole(Role.REVIEWER) must beTrue
  	    val u = User.find(user.id).get
  	    u.roles.contains(Role.REVIEWER) must beTrue
  	    u.hasRole(Role.REVIEWER) must beTrue
  	    u.hasRole(Role.ADMIN) must beFalse
  	  }
  	}
  	
  	"remove a role" in {
  	  running(FakeApplication()) {
  	    val user = createAndGetUser
  	    user.addRole(Role.USER)
  	    user.addRole(Role.REVIEWER) must beTrue
  	    User.find(user.id).get.roles.contains(Role.USER) must beTrue
  	    User.find(user.id).get.removeRole(Role.USER) must beTrue
  	    val roles = User.find(user.id).get.roles
  	    roles.contains(Role.USER) must beFalse
  	    roles.contains(Role.REVIEWER) must beTrue
  	  }
  	}
  	
  	"find a user" in {
  	  running(FakeApplication()) {
  	    val user = createAndGetUser
  	    User.find(user.id) must beSome
  	  }
  	}
  	
  	"find all users" in {
  	  running(FakeApplication()) {
  	    val user = createAndGetUser
  	    val all = User.all
  	    all.nonEmpty must beTrue
  	    all.map(_.id).contains(user.id) must beTrue
  	  }
  	}
  	
  	"find a user by username/email" in {
  	  running(FakeApplication()) {
  	    val user = createAndGetUser
  	    User.findByUsername(user.username) must beSome
  	    User.findByEmail(user.email) must beSome
  	  }
  	}
  	
  	"update on login" in {
  	  running(FakeApplication()) {
  	    val user = createAndGetUser
  	    val origCount = user.logins
  	    val origTime = user.lastLogin
  	    user.updateLoginCount() must beTrue
  	    User.find(user.id).get.logins must be_>(origCount)
  	    User.find(user.id).get.lastLogin must be_>(origTime)
  	  }
  	}
    
  }

}