package models

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class BlacklistEventSpec extends Specification {
  
  private val source = "SBW"
  private def validUri: Uri = Uri.findOrCreate(UriSpec.reportedUri).get
  private def reportedEvent: ReportedEvent = {
    ReportedEvent(validUri.id, source, System.currentTimeMillis/1000, Some(System.currentTimeMillis/1000))
  }
  
  "BlacklistEvent" should {
    
    "create a blacklist event" in {
      running(FakeApplication()) {
        BlacklistEvent.create(reportedEvent) must be equalTo(true)
      }
    }
    
    "find a blacklist event" in {
      running(FakeApplication()) {
        val reported = reportedEvent
        val createdEvent = BlacklistEvent.create(reported)
        createdEvent must be equalTo(true)	//TODO WTSN-11
      }
    }
    
    "delete a blacklist event" in {
      running(FakeApplication()) {
        true must be equalTo(false)	//TODO WTSN-11
      }
    }
    
    "update a blacklist event" in {
      running(FakeApplication()) {
        true must be equalTo(false)	//TODO WTSN-11
      }
    }
    
    
  }

}