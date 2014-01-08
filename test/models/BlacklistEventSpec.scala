package models

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class BlacklistEventSpec extends Specification {
  
  private val source = Source.SBW
  private val numInBulk = 20
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
    
    "update a blacklist event" in {
      running(FakeApplication()) {
        val now = System.currentTimeMillis / 1000
        val uriId = validUri.id
        val initialEvent = ReportedEvent(uriId, source, now)
        BlacklistEvent.createOrUpdate(initialEvent) must beTrue
        BlacklistEvent.findBlacklistedByUri(uriId, Some(source)).size must be equalTo(1)
        val updatedTime = now - 10
        val updatedEvent = ReportedEvent(uriId, source, updatedTime)
        BlacklistEvent.createOrUpdate(updatedEvent) must beTrue
        val events = BlacklistEvent.findBlacklistedByUri(uriId, Some(source))
        events.size must be equalTo(1)
        events.head.blacklistedAt must be equalTo(updatedTime)
      }
    }    
    
    "create and/or update blacklist events in bulk" in {
      running(FakeApplication()) {
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
    
    "update no longer blacklisted from new blacklist" in {
      running(FakeApplication()) {
        val timeA = System.currentTimeMillis / 1000
        val blacklistA = (1 to numInBulk).foldLeft(List.empty[ReportedEvent]) { (list, _) =>
          list :+ ReportedEvent(validUri.id, source, timeA, None)
        }
        val timeB = timeA + 10
        val blacklistB = (1 to numInBulk).foldLeft(List.empty[ReportedEvent]) { (list, _) =>
          list :+ ReportedEvent(validUri.id, source, timeB, None)
        }
        BlacklistEvent.createOrUpdate(blacklistA, source) must be equalTo(blacklistA.size)
        val updated = BlacklistEvent.updateNoLongerBlacklisted(blacklistB.map(_.uriId).toSet, source, timeB)
        updated must be_>=(blacklistA.size)
        blacklistA.map { event => 
          BlacklistEvent.findBlacklistedByUri(event.uriId, Some(event.source)).isEmpty must beTrue
        }
      }
    }
    
    "find currently blacklisted Uri IDs" in {
      running(FakeApplication()) {
        val time = System.currentTimeMillis / 1000
        val blacklist = (1 to numInBulk).foldLeft(List.empty[ReportedEvent]) { (list, _) =>
          list :+ ReportedEvent(validUri.id, source, time, None)
        }
        BlacklistEvent.createOrUpdate(blacklist, source) must be equalTo(blacklist.size)
        val uriIds = BlacklistEvent.blacklistedUriIdsEventIds(time+1, Some(source)).keySet
        blacklist.map(event => uriIds.contains(event.uriId) must beTrue)
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