package workers

import java.io.File
import akka.actor.ActorSystem
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit
import play.api.{DefaultApplication, Logger, Mode, Play}
import play.api.libs.concurrent.Execution.Implicits._
import controllers.{Blacklist, Redis}
import models.Source

object Scheduler {
  	
  def main(args: Array[String]): Unit = {
    Play.start(new DefaultApplication(new File("."), Scheduler.getClass.getClassLoader, None, Mode.Prod))
    val interval = new FiniteDuration(60, TimeUnit.SECONDS)
    val system = ActorSystem("ImportBlacklistQueue")
    system.scheduler.schedule(Duration.Zero, interval, BlacklistQueue())
  }
  
}

case class BlacklistQueue() extends Runnable {
  
  def blacklists: List[Blacklist] = {
    val sourcesWithDifferential = List(Source.GOOG, Source.TTS)
    return sourcesWithDifferential.map { source =>
    	Redis.blacklistTimes(source).foldLeft(List.empty[Blacklist]) { (list, time) =>
      	list :+ Blacklist(source, time, Redis.getBlacklist(source, time))
    	}
    }.flatten
  }
  
  def importQueue() = {
		blacklists.sortBy(_.time).foreach { blacklist =>
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