package models.enums

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class RoleSpec extends Specification {
  
  private val user = "USER"
  
  "Role" should {
    
    "match string to Role" in {
      Role.fromStr("") must beNone
      Role.fromStr(user.toLowerCase) must beSome
      Role.fromStr(user.toUpperCase) must beSome
      val role = Role.fromStr(user)
      role must beSome
      role.get must equalTo(Role.USER)
      role.get must not equalTo(Role.ADMIN)
    }
    
  }

}