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
        BlacklistEvent.createOrUpdate(blacklistedEvent) must be equalTo(true)
        BlacklistEvent.createOrUpdate(unblacklistedEvent) must be equalTo(true)
      }
    }
    
    "find a blacklist event" in {
      running(FakeApplication()) {
        val reported = blacklistedEvent
        BlacklistEvent.createOrUpdate(reported) must be equalTo(true)
        BlacklistEvent.findByUri(reported.uriId).nonEmpty must be equalTo(true)
      }
    }
    
    "remove a blacklist event" in {
      running(FakeApplication()) {
        val reported = blacklistedEvent
        BlacklistEvent.createOrUpdate(reported) must be equalTo(true)
        val event = BlacklistEvent.findByUri(reported.uriId)
        event.nonEmpty must be equalTo(true)
        event.head.delete()
        BlacklistEvent.findByUri(reported.uriId).isEmpty must be equalTo(true)
      }
    }
    
    "update a blacklist event" in {
      running(FakeApplication()) {
        val uri = validUri
        val now = System.currentTimeMillis / 1000
        val older = ReportedEvent(uri.id, source, now - 1138)
        val newer = ReportedEvent(uri.id, source, now - 47)
        val clean = ReportedEvent(uri.id, source, now, Some(now))
        
        BlacklistEvent.createOrUpdate(newer) must be equalTo(true)
        val newerEvent = BlacklistEvent.findByUri(uri.id)
        newerEvent.nonEmpty must be equalTo(true)
        newerEvent.head.blacklisted must be equalTo(true)
        
        BlacklistEvent.createOrUpdate(older) must be equalTo(true)
        BlacklistEvent.findByUri(uri.id).head.blacklistedAt must be equalTo(older.blacklistedAt)
        
        BlacklistEvent.createOrUpdate(clean) must be equalTo(true)
        val cleanedEvent = BlacklistEvent.findByUri(clean.uriId).head
        cleanedEvent.blacklistedAt must be equalTo(older.blacklistedAt)
        cleanedEvent.unblacklistedAt must beSome
        cleanedEvent.unblacklistedAt.get must be equalTo(now)
        cleanedEvent.blacklisted must be equalTo(false)
      }
    }
    
    
  }

}