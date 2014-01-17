package models.enums

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class ClosedReasonSpec extends Specification {
  
  private val admin = "ADMINISTRATIVE"
  
  "ClosedReason" should {
    
    "match string to reason" in {
      ClosedReason.fromStr("") must beNone
      ClosedReason.fromStr(admin.toUpperCase) must beSome
      ClosedReason.fromStr(admin.toLowerCase) must beSome
      val reason = ClosedReason.fromStr(admin)
      reason must beSome
      reason.get must not equalTo(ClosedReason.ABUSIVE)
      reason.get must equalTo(ClosedReason.ADMINISTRATIVE)
      ClosedReason.ADMINISTRATIVE must not equalTo(ClosedReason.ABUSIVE)
      ClosedReason.ADMINISTRATIVE must equalTo(ClosedReason.ADMINISTRATIVE)
    }
    
  }

}