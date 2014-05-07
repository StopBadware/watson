package workers

import java.io.File
import akka.actor.ActorSystem
import scala.concurrent.duration._
import scala.util.Try
import java.util.concurrent.TimeUnit
import play.api.{DefaultApplication, Logger, Mode, Play}
import play.api.libs.concurrent.Execution.Implicits._
import io.iron.ironmq.{Client, Cloud, Queue}
import controllers.{Blacklist, Mailer, Redis}
import models.enums.Source

object Scheduler {
  	
  def main(args: Array[String]): Unit = {
    Play.start(new DefaultApplication(new File("."), Scheduler.getClass.getClassLoader, None, Mode.Prod))
    val everyMinute = new FiniteDuration(1, TimeUnit.MINUTES)
    val everyHour = new FiniteDuration(1, TimeUnit.HOURS)
    val resolverHours = new FiniteDuration(Try(sys.env("RESOLVER_INTERVAL_HOURS").toLong).getOrElse(12), TimeUnit.HOURS)
    
    val importBlacklistSystem = ActorSystem("ImportBlacklistQueue")
    importBlacklistSystem.scheduler.schedule(Duration.Zero, everyMinute, BlacklistQueue())
    
    val sendGoogleRescanQueue = ActorSystem("SendGoogleRescanQueue")
    sendGoogleRescanQueue.scheduler.schedule(Duration.Zero, everyHour, GoogleRescanQueue())
    
    val ipAsResolver = ActorSystem("IpAsResolver")
    ipAsResolver.scheduler.schedule(Duration.Zero, resolverHours, IpAsResolver())
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
      if (queue.nonEmpty) {
      	Logger.error("Sending rescan requests to Google failed")
      }
      0
    }
  }
  
  def run() = sendQueue()
  
}

case class IpAsResolver() extends Runnable {
  
  private val ironMqProjectId = sys.env("IRON_MQ_PROJECT_ID")
  private val ironMqToken = sys.env("IRON_MQ_TOKEN")
  private val cloud = Cloud.ironAWSUSEast
  
  def addResolveRequestToQueue(): Boolean = {
    val client = new Client(ironMqProjectId, ironMqToken, cloud)
    val queue = client.queue("resolve_queue")
    val added = Try(queue.push("WTSN")).isSuccess
    
    if (added) {
      Logger.info("Added resolve request for WTSN to resolver queue")
    } else {
    	Logger.error("Adding message to resovler queue failed")
    }
    return added
  }
  
  def run() = addResolveRequestToQueue()
  
}