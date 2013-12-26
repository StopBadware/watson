package models

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class BlacklistEventSpec extends Specification {
  
  private val source = Source.SBW
  private def validUri: Uri = Uri.findOrCreate(UriSpec.validUri).get
  private def blacklistedEvent: ReportedEvent = {
    ReportedEvent(validUri.id, source, System.currentTimeMillis/1000)
  }
  private def unblacklistedEvent: ReportedEvent = {
    ReportedEvent(validUri.id, source, System.currentTimeMillis/1000, Some(System.currentTimeMillis/1000))
  }
  
  "BlacklistEvent" should {
    
    "create a blacklist event" in {
      running(FakeApplication()) {
        BlacklistEvent.createOrUpdate(blacklistedEvent) must beTrue
        BlacklistEvent.createOrUpdate(unblacklistedEvent) must beTrue
      }
    }
    
    "create and/or update blacklist events in bulk" in {
      running(FakeApplication()) {
        val numInBulk = 10
        val events = (1 to numInBulk).foldLeft(List.empty[ReportedEvent]) { (list, _) =>
          list :+ blacklistedEvent
        }
        events.splitAt(numInBulk / 2)._1.foreach(BlacklistEvent.createOrUpdate(_))
        BlacklistEvent.createOrUpdate(events, source) must be equalTo(events.size)
      }
    }    
    
    "find blacklist events" in {
      running(FakeApplication()) {
        val reported = blacklistedEvent
        BlacklistEvent.createOrUpdate(reported) must beTrue
        BlacklistEvent.findByUri(reported.uriId).nonEmpty must beTrue
      }
    }
    
    "find blacklist events by source" in {
      running(FakeApplication()) {
        val reported = blacklistedEvent
        BlacklistEvent.createOrUpdate(reported) must beTrue
        BlacklistEvent.findByUri(reported.uriId, Some(source)).nonEmpty must beTrue
      }
    }    
    
    "find currently blacklisted events" in {
      running(FakeApplication()) {
        val reported = blacklistedEvent
        BlacklistEvent.createOrUpdate(reported) must beTrue
        BlacklistEvent.findBlacklistedByUri(reported.uriId).nonEmpty must beTrue
      }
    }  
    
    "find currently blacklisted events by source" in {
      running(FakeApplication()) {
        val reported = blacklistedEvent
        BlacklistEvent.createOrUpdate(reported) must beTrue
        BlacklistEvent.findByUri(reported.uriId).nonEmpty must beTrue
        BlacklistEvent.findBlacklistedByUri(reported.uriId, Some(source)).nonEmpty must beTrue
      }
    }     
    
    "remove a blacklist event" in {
      running(FakeApplication()) {
        val reported = blacklistedEvent
        BlacklistEvent.createOrUpdate(reported) must beTrue
        val event = BlacklistEvent.findByUri(reported.uriId)
        event.nonEmpty must beTrue
        event.head.delete()
        BlacklistEvent.findByUri(reported.uriId).isEmpty must beTrue
      }
    }
    
    "update a blacklist event" in {
      running(FakeApplication()) {
        val uri = validUri
        val now = System.currentTimeMillis / 1000
        val older = ReportedEvent(uri.id, source, now - 1138)
        val newer = ReportedEvent(uri.id, source, now - 47)
        val clean = ReportedEvent(uri.id, source, now, Some(now))
        
        BlacklistEvent.createOrUpdate(newer) must beTrue
        val newerEvent = BlacklistEvent.findByUri(uri.id)
        newerEvent.nonEmpty must beTrue
        newerEvent.head.blacklistedAt must be equalTo(newer.blacklistedAt)
        newerEvent.head.unblacklistedAt must beNone
        newerEvent.head.blacklisted must beTrue
        
        BlacklistEvent.createOrUpdate(older) must beTrue
        BlacklistEvent.findByUri(uri.id).head.blacklistedAt must be equalTo(older.blacklistedAt)
        
        BlacklistEvent.createOrUpdate(clean) must beTrue
        val cleanedEvent = BlacklistEvent.findByUri(clean.uriId).head
        cleanedEvent.blacklistedAt must be equalTo(older.blacklistedAt)
        cleanedEvent.unblacklistedAt must beSome
        cleanedEvent.unblacklistedAt.get must be equalTo(now)
        cleanedEvent.blacklisted must beFalse
      }
    }
    
    "unblacklist an event" in {
      running(FakeApplication()) {
        val uri = validUri
        val now = System.currentTimeMillis / 1000
        val dirty = ReportedEvent(uri.id, source, now - 42)
        
        BlacklistEvent.createOrUpdate(dirty) must beTrue
        val blacklistEvent = BlacklistEvent.findByUri(uri.id)
        blacklistEvent.nonEmpty must beTrue
        blacklistEvent.head.blacklistedAt must be equalTo(dirty.blacklistedAt)
        blacklistEvent.head.unblacklistedAt must beNone
        blacklistEvent.head.blacklisted must beTrue
        
        BlacklistEvent.markNoLongerBlacklisted(uri.id, source, now) must beTrue
        val unblacklistEvent = BlacklistEvent.findByUri(uri.id).head
        unblacklistEvent.unblacklistedAt must beSome
        unblacklistEvent.unblacklistedAt.get must be equalTo(now)
        unblacklistEvent.blacklisted must beFalse
      }
    }
    
    "finds the most recent time for a source" in {
      running(FakeApplication()) {
        val event = blacklistedEvent
        BlacklistEvent.createOrUpdate(event) must beTrue
        BlacklistEvent.timeOfLast(source) must be equalTo(event.blacklistedAt)
      }
    }    
    
  }

}