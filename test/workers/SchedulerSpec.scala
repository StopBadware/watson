package workers

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._
import controllers.{Blacklist, BlacklistSpec, Hash, Redis}
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
        val urls = List(urlA, urlB)
	      Blacklist.importBlacklist(BlacklistSpec.json(time, urls), source)
	      val fromQueue = Redis.getBlacklist(source, time)
	      fromQueue.nonEmpty must beTrue
	      val queueCheck = BlacklistQueue()
	      val blacklists = queueCheck.blacklists
	      blacklists.nonEmpty must beTrue
	      blacklists.keySet.contains(source) must beTrue
	      blacklists(source).contains(time) must beTrue
	      blacklists(source).filter(_ == time).map { blTime =>
	        val blacklist = Redis.getBlacklist(source, blTime)
          blacklist.size must equalTo(urls.size)
          blacklist.contains(urlA) must beTrue
          blacklist.contains(urlB) must beTrue
        }
      }      
    }
    
    "import blacklists from queue" in {
      running(FakeApplication()) {
        val time = System.currentTimeMillis / 1000
        val urlA = "example.com"
        val urlB = "http://www.example"+time+".com/"
	      Blacklist.importBlacklist(BlacklistSpec.json(time, List(urlA, urlB)), source)
	      val fromQueue = Redis.getBlacklist(source, time)
	      fromQueue.nonEmpty must beTrue
	      val queueCheck = BlacklistQueue()
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