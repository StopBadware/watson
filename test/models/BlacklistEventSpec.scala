package models

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class BlacklistEventSpec extends Specification {
  
  private val source = Source.SBW
  private def validUri: Uri = Uri.findOrCreate(UriSpec.reportedUri).get
  private def blacklistedEvent: ReportedEvent = {
    ReportedEvent(validUri.id, source, System.currentTimeMillis/1000)
  }
  private def unblacklistedEvent: ReportedEvent = {
    ReportedEvent(validUri.id, source, System.currentTimeMillis/1000, Some(System.currentTimeMillis/1000))
  }
  
  "BlacklistEvent" should {
    
    "create a blacklist event" in {
      running(FakeApplication()) {
        BlacklistEvent.create(blacklistedEvent) must be equalTo(true)
        BlacklistEvent.create(unblacklistedEvent) must be equalTo(true)
      }
    }
    
    "find a blacklist event" in {
      running(FakeApplication()) {
        val reported = blacklistedEvent
        BlacklistEvent.create(reported) must be equalTo(true)
        BlacklistEvent.findByUri(reported.uriId).nonEmpty must be equalTo(true)
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