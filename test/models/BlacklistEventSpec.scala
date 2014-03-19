package models

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._
import models.enums.Source

@RunWith(classOf[JUnitRunner])
class BlacklistEventSpec extends Specification {
  
  private val source = Source.NSF
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
        BlacklistEvent.findBlacklistedByUri(uriId, Some(source)).size must equalTo(1)
        val updatedTime = now - 10
        val updatedEvent = ReportedEvent(uriId, source, updatedTime)
        BlacklistEvent.createOrUpdate(updatedEvent) must beTrue
        val events = BlacklistEvent.findBlacklistedByUri(uriId, Some(source))
        events.size must equalTo(1)
        events.head.blacklistedAt must equalTo(updatedTime)
      }
    }
    
    "create blacklist events in bulk" in {
      running(FakeApplication()) {
        val uris = (1 to numInBulk).foldLeft(List.empty[Int]) { (list, _) =>
          list :+ validUri.id
        }
        BlacklistEvent.create(uris, source, System.currentTimeMillis / 1000, None) must equalTo(uris.size)
        uris.map { id =>
          BlacklistEvent.findByUri(id, Some(source)).nonEmpty must beTrue
        }
      }
    }
    
    "update blacklist events in bulk" in {
      running(FakeApplication()) {
        val time = System.currentTimeMillis / 1000
        val uris = (1 to numInBulk).foldLeft(List.empty[Int]) { (list, _) =>
          list :+ validUri.id
        }
        BlacklistEvent.create(uris, source, time, None) must equalTo(uris.size)
        val blEvents = uris.map(id => BlacklistEvent.findByUri(id, Some(source))).flatten
        val newBlTime = blEvents.foldLeft(time) { (min, event) =>
          if (event.blacklistedAt < min) event.blacklistedAt else min
        } - 10
        BlacklistEvent.update(blEvents.map(_.id).toSet, newBlTime) must equalTo(uris.size)
        blEvents.map { event =>
          BlacklistEvent.findByUri(event.uriId, Some(source)).head.blacklistedAt must not equalTo(event.blacklistedAt)
          BlacklistEvent.findByUri(event.uriId, Some(source)).head.blacklistedAt must equalTo(newBlTime)
        }
      }
    }
    
    "update blacklist event times" in {
      running(FakeApplication()) {
        val time = System.currentTimeMillis / 1000
        val uris = (1 to numInBulk).foldLeft(List.empty[Int]) { (list, _) =>
          list :+ validUri.id
        }
        BlacklistEvent.create(uris, source, time, None) must equalTo(uris.size)
        val blEvents = uris.map(id => BlacklistEvent.findByUri(id, Some(source))).flatten
        val newBlTime = blEvents.foldLeft(time) { (min, event) =>
          if (event.blacklistedAt < min) event.blacklistedAt else min
        } - 10
        val updated = BlacklistEvent.updateBlacklistTime(uris, newBlTime, source)
        updated must equalTo(uris.size)
        blEvents.map { event =>
          BlacklistEvent.find(event.id).get.blacklistedAt must not equalTo(event.blacklistedAt)
          BlacklistEvent.find(event.id).get.blacklistedAt must equalTo(newBlTime)
        }
      }
    }
    
    "update event times from a provided blacklist" in {
      running(FakeApplication()) {
        val time = System.currentTimeMillis / 1000
        val uris = (1 to numInBulk).foldLeft(List.empty[Int])((list, _) => list :+ validUri.id)
        BlacklistEvent.create(uris, source, time, None) must equalTo(uris.size)
        uris.map(id => BlacklistEvent.findByUri(id, Some(source))).flatten.forall(_.blacklistedAt==time) must beTrue
        val updatedTime = time - 1
        BlacklistEvent.updateBlacklistTime(uris, updatedTime, source)
        uris.map(id => BlacklistEvent.findByUri(id, Some(source))).flatten.forall(_.blacklistedAt==updatedTime) must beTrue
      }
    }    
    
    "unblacklist events in bulk" in {
      running(FakeApplication()) {
        val uris = (1 to numInBulk).foldLeft(List.empty[Int]) { (list, _) =>
          list :+ validUri.id
        }
        BlacklistEvent.create(uris, source, System.currentTimeMillis / 1000, None) must equalTo(uris.size)
        val blEvents = uris.map(id => BlacklistEvent.findByUri(id, Some(source))).flatten
        blEvents.forall(_.blacklisted) must beTrue
        BlacklistEvent.unBlacklist(blEvents.map(_.id), System.currentTimeMillis / 1000) must equalTo(blEvents.size)
        uris.map(BlacklistEvent.findByUri(_, Some(source)).head).forall(_.blacklisted) must beFalse
      }
    }
    
    "unblacklist events not on a provided blacklist" in {
      running(FakeApplication()) {
        val time = System.currentTimeMillis / 1000
        val urisA = (1 to numInBulk).foldLeft(List.empty[Int])((list, _) => list :+ validUri.id)
        val urisB = (1 to numInBulk).foldLeft(List.empty[Int])((list, _) => list :+ validUri.id)
        BlacklistEvent.create(urisA, source, time, None) must equalTo(urisA.size)
        urisA.map(id => BlacklistEvent.findByUri(id, Some(source))).flatten.forall(_.blacklisted) must beTrue
        BlacklistEvent.updateNoLongerBlacklisted(source, time+1, urisB)
        urisA.map(id => BlacklistEvent.findByUri(id, Some(source))).flatten.forall(_.blacklisted) must beFalse
        urisB.map(id => BlacklistEvent.findByUri(id, Some(source))).flatten.forall(_.blacklisted) must beTrue
      }
    }
    
    "find blacklist event by id" in {
      running(FakeApplication()) {
        val reported = blacklistedEvent
        BlacklistEvent.createOrUpdate(reported) must beTrue
        val findByUri = BlacklistEvent.findByUri(reported.uriId)
        findByUri.nonEmpty must beTrue
        val head = findByUri.head
        val event = BlacklistEvent.find(head.id).get
        event.blacklisted must equalTo(head.blacklisted)
        event.blacklistedAt must equalTo(head.blacklistedAt)
        event.unblacklistedAt must equalTo(head.unblacklistedAt)
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
        event.head.delete() must beTrue
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
        newerEvent.head.blacklistedAt must equalTo(newer.blacklistedAt)
        newerEvent.head.unblacklistedAt must beNone
        newerEvent.head.blacklisted must beTrue
        
        BlacklistEvent.createOrUpdate(older) must beTrue
        BlacklistEvent.findByUri(uri.id).head.blacklistedAt must equalTo(older.blacklistedAt)
        
        BlacklistEvent.createOrUpdate(clean) must beTrue
        val cleanedEvent = BlacklistEvent.findByUri(clean.uriId).head
        cleanedEvent.blacklistedAt must equalTo(older.blacklistedAt)
        cleanedEvent.unblacklistedAt must beSome
        cleanedEvent.unblacklistedAt.get must equalTo(now)
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
        blacklistEvent.head.blacklistedAt must equalTo(dirty.blacklistedAt)
        blacklistEvent.head.unblacklistedAt must beNone
        blacklistEvent.head.blacklisted must beTrue
        
        BlacklistEvent.unBlacklist(uri.id, source, now) must beTrue
        val unblacklistEvent = BlacklistEvent.findByUri(uri.id).head
        unblacklistEvent.unblacklistedAt must beSome
        unblacklistEvent.unblacklistedAt.get must equalTo(now)
        unblacklistEvent.blacklisted must beFalse
      }
    }
    
    "find currently blacklisted Uri IDs" in {
      running(FakeApplication()) {
        val time = System.currentTimeMillis / 1000
        val blacklist = (1 to numInBulk).foldLeft(List.empty[ReportedEvent]) { (list, _) =>
          list :+ ReportedEvent(validUri.id, source, time, None)
        }
        blacklist.foreach(BlacklistEvent.createOrUpdate(_))
        val uriIds = BlacklistEvent.blacklistedUriIdsEventIds(source, Some(time+1)).keySet
        blacklist.map(event => uriIds.contains(event.uriId) must beTrue)
      }
    }
    
    "finds the most recent time for a source" in {
      running(FakeApplication()) {
        val event = blacklistedEvent
        BlacklistEvent.createOrUpdate(event) must beTrue
        BlacklistEvent.timeOfLast(source) must equalTo(event.blacklistedAt)
      }
    }    
    
  }

}