package workers

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._
import controllers.{Blacklist, Redis}
import models.Source

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
	      fromQueue.isDefined must beTrue
	      val queueCheck = CheckQueue(source)
	      val queued = queueCheck.blacklistQueue
	      queued.nonEmpty must beTrue
	      queued.map { blacklist =>
          blacklist.source must be equalTo(source)
          blacklist.time must be equalTo(time)
          blacklist.urls.size must be equalTo(2)
          blacklist.urls.contains(urlA) must beTrue
          blacklist.urls.contains(urlB) must beTrue
        }
      }      
    }
    
  }

}