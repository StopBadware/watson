package workers

import akka.actor.ActorSystem
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._
import controllers.{Blacklist, Redis}
import models.Source

object Scheduler {
  	
  def main(args: Array[String]): Unit = {
    val interval = new FiniteDuration(30, TimeUnit.SECONDS)
    val system = ActorSystem("CheckBlacklistQueue")
    val sourcesWithDifferential = List(Source.GOOG, Source.NSF)
    sourcesWithDifferential.foreach { source =>
    	system.scheduler.schedule(Duration.Zero, interval, BlacklistQueue(source))
    }
  }
  
}

case class BlacklistQueue(source: Source) extends Runnable {
  
  def blacklists: List[Blacklist] = {
    return Redis.blacklistTimes(source).foldLeft(List.empty[Blacklist]) { (list, time) =>
      list :+ Blacklist(source, time, Redis.getBlacklist(source, time))
    }
  }
  
  def importQueue() = {
		blacklists.foreach { blacklist =>
		  val success = Blacklist.importDifferential(blacklist.urls, blacklist.source, blacklist.time)
		  if (success || blacklist.urls.isEmpty) {
		    Redis.dropBlacklist(blacklist.source, blacklist.time)
		  } else {
		    Logger.error("Importing non-empty blacklist ("+blacklist+") from queue failed")
		  }
		}    
  }
  
  def run() = importQueue()
  
}