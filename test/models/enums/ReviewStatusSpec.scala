package models.enums

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class ReviewStatusSpec extends Specification {
  
  private val cwor = "CLOSED_WITHOUT_REVIEW"
  
  "ReviewStatus" should {
    
    "match string to status" in {
      ReviewStatus.fromStr("") must beNone
      ReviewStatus.fromStr(cwor.toUpperCase) must beSome
      ReviewStatus.fromStr(cwor.toLowerCase) must beSome
      val status = ReviewStatus.fromStr(cwor)
      status must beSome
      status.get must not equalTo(ReviewStatus.PENDING)
      status.get must equalTo(ReviewStatus.CLOSED_WITHOUT_REVIEW)
      ReviewStatus.CLOSED_WITHOUT_REVIEW must not equalTo(ReviewStatus.PENDING)
      ReviewStatus.CLOSED_WITHOUT_REVIEW must equalTo(ReviewStatus.CLOSED_WITHOUT_REVIEW)
    }
    
    "determines if status represents a state of open" in {
      ReviewStatus.BAD.isOpen must beFalse
      ReviewStatus.CLEAN.isOpen must beFalse
      ReviewStatus.CLOSED_WITHOUT_REVIEW.isOpen must beFalse
      ReviewStatus.NEW.isOpen must beTrue
      ReviewStatus.PENDING.isOpen must beTrue
      ReviewStatus.REJECTED.isOpen must beTrue
      ReviewStatus.REOPENED.isOpen must beTrue
    }
    
  }

}