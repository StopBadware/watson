package workers

import akka.actor.ActorSystem
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit
import play.api.libs.concurrent.Execution.Implicits._
import controllers.{Blacklist, Redis}

object Scheduler {
  	
  def main(args: Array[String]): Unit = {
    val interval = new FiniteDuration(30, TimeUnit.SECONDS)
    val system = ActorSystem("CheckBlacklistQueue")
    system.scheduler.schedule(Duration.Zero, interval, CheckBlacklistQueue())
  }
  
}

case class CheckBlacklistQueue() extends Runnable {
  
  def blacklistQueue: List[Blacklist] = {
    List()	//TODO WTSN-39 check redis queue
  }
  
  def run() = {
		val blacklists = blacklistQueue
		blacklistQueue.foreach { blacklist =>
		  Blacklist.importDifferential(blacklist.urls, blacklist.source, blacklist.time)
		}
  }
  
}