package workers

import java.io.File
import akka.actor.ActorSystem
import scala.concurrent.duration._
import java.util.concurrent.TimeUnit
import play.api.{DefaultApplication, Logger, Mode, Play}
import play.api.libs.concurrent.Execution.Implicits._
import controllers.{Blacklist, Mailer, Redis}
import models.enums.Source

object Scheduler {
  	
  def main(args: Array[String]): Unit = {
    Play.start(new DefaultApplication(new File("."), Scheduler.getClass.getClassLoader, None, Mode.Prod))
    val minute = new FiniteDuration(1, TimeUnit.MINUTES)
    val hour = new FiniteDuration(1, TimeUnit.HOURS)
    val importBlacklistSystem = ActorSystem("ImportBlacklistQueue")
    importBlacklistSystem.scheduler.schedule(Duration.Zero, minute, BlacklistQueue())
    val sendGoogleRescanQueue = ActorSystem("SendGoogleRescanQueue")
    sendGoogleRescanQueue.scheduler.schedule(Duration.Zero, hour, GoogleRescanQueue())
  }
  
}

case class BlacklistQueue() extends Runnable {
  
  def blacklists: Map[Source, List[Long]] = {
    val sourcesWithDifferential = List(Source.GOOG, Source.TTS)
    return sourcesWithDifferential.map(source => source -> Redis.blacklistTimes(source)).toMap
  }
  
  def importQueue() = {
    blacklists.foreach { case (source, times) =>
      times.sortWith(_ < _).foreach { time =>
        val blacklist = Redis.getBlacklist(source, time)
        val addUpdRem = Blacklist.importDifferential(blacklist, source, time)
        if (addUpdRem==0 && blacklist.nonEmpty) {
        	Logger.warn("Importing blacklist from "+source+" ("+time+") had no additions, updates, or removals")
        }
        Redis.dropBlacklist(source, time)
      }
    }
  }
  
  def run() = importQueue()
  
}

case class GoogleRescanQueue() extends Runnable {
  
  def sendQueue(): Int = {
    val queue = Redis.getGoogleRescanQueue
    return if (Mailer.sendGoogleRescanRequest(queue)()) {
      Redis.removeFromGoogleRescanQueue(queue)
      Logger.info("Sent " + queue.size + " rescan requests to Google")
      queue.size
    } else {
      Logger.error("Sending rescan requests to Google failed")
      0
    }
  }
  
  def run() = sendQueue()
  
}