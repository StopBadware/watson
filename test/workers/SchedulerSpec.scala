package workers

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._
import controllers.{Blacklist, Hash, Redis}
import models.{BlacklistEvent, Source, Uri}

@RunWith(classOf[JUnitRunner])
class SchedulerSpec extends Specification {
  
  private val source = Source.GOOG
  
  "Scheduler" should {
    
    "retrieve and unmarshall differential blacklist from queue" in {
      running(FakeApplication()) {
        val time = System.currentTimeMillis / 1000
        val urlA = "example.com"
        val urlB = "http://www.example.com/" + time
	      val json = "[{\"url\":\""+urlB+"\",\"time\":"+time+"}, {\"url\":\""+urlA+"\",\"time\":"+time+"}]"
	      Blacklist.importBlacklist(json, source)
	      val fromQueue = Redis.getBlacklist(source, time)
	      fromQueue.nonEmpty must beTrue
	      val queueCheck = BlacklistQueue(source)
	      val blacklists = queueCheck.blacklists
	      blacklists.nonEmpty must beTrue
	      blacklists.map(_.source).contains(source) must beTrue
	      blacklists.map(_.time).contains(time) must beTrue
	      blacklists.filter(bl => bl.source==source && bl.time==time).map { blacklist =>
          blacklist.urls.size must be equalTo(2)
          blacklist.urls.contains(urlA) must beTrue
          blacklist.urls.contains(urlB) must beTrue
        }
      }      
    }
    
    "import blacklists from queue" in {
      running(FakeApplication()) {
        val time = System.currentTimeMillis / 1000
        val urlA = "example.com"
        val urlB = "http://www.example"+time+".com/"
	      val json = "[{\"url\":\""+urlB+"\",\"time\":"+time+"}, {\"url\":\""+urlA+"\",\"time\":"+time+"}]"
	      Blacklist.importBlacklist(json, source)
	      val fromQueue = Redis.getBlacklist(source, time)
	      fromQueue.nonEmpty must beTrue
	      val queueCheck = BlacklistQueue(source)
	      queueCheck.blacklists.nonEmpty must beTrue
	      queueCheck.importQueue()
	      val uri = Uri.find(Hash.sha256(urlB).get)
	      uri.isDefined must beTrue
	      val events = BlacklistEvent.findByUri(uri.get.id, Some(source))
	      events.nonEmpty must beTrue
	      events.filter(_.blacklistedAt == time).nonEmpty must beTrue
      }      
    }    
    
  }

}