package workers

import java.io.File
import akka.actor.ActorSystem
import scala.collection.JavaConversions._
import scala.concurrent.duration._
import scala.util.Try
import java.util.concurrent.TimeUnit
import play.api.{DefaultApplication, Logger, Mode, Play}
import play.api.libs.concurrent.Execution.Implicits._
import io.iron.ironmq.{Client, Cloud, Queue}
import com.fasterxml.jackson.databind.JsonNode
import controllers.{Blacklist, Host, JsonMapper, Mailer, Redis}
import models.{AutonomousSystem, HostIpMapping, IpAsnMapping}
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
    
    val addResolverRequest = ActorSystem("AddResolverRequest")
    addResolverRequest.scheduler.schedule(Duration.Zero, resolverHours, AddResolverRequest())
    
    val importResolverResults = ActorSystem("ImportResolverResults")
    importResolverResults.scheduler.schedule(Duration.Zero, everyMinute, ImportResolverResults())
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

case class AddResolverRequest() extends Runnable {
  
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

case class ImportResolverResults() extends Runnable with JsonMapper {
  
  def importResolverResults(json: JsonNode): Boolean = {
	    val asOf = json.get("time").asLong
	    val ipsAsns = json.get("ip_to_as").fields.toList
	    
	    val asWrites = ipsAsns.map(_.getValue).foldLeft(0) { (writes, asInfo) =>
	      val wrote = AutonomousSystem.createOrUpdate(asInfo.get("asn").asInt, asInfo.get("name").asText, asInfo.get("country").asText)
	      if (wrote) writes + 1 else writes
	    }
	    Logger.info("Added or updated " + asWrites + " Autonomous Systems")
	    
	    val ipAsWrites = ipsAsns.foldLeft(0) { (writes, ipAsinfo) =>
	      val wrote = IpAsnMapping.create(ipAsinfo.getKey.toLong, ipAsinfo.getValue.get("asn").asInt, asOf)
	      if (wrote) writes + 1 else writes
	    }
	    Logger.info("Wrote " + ipAsWrites + " IP=>AS mappings")
	    
	    val hostIpWrites = json.get("host_to_ip").fields.toList.foldLeft(0) { (writes, hostIpInfo) =>
	      val wrote = HostIpMapping.create(Host.reverse(hostIpInfo.getKey.toString), hostIpInfo.getValue.asLong, asOf)
	      if (wrote) writes + 1 else writes
	    }
	    Logger.info("Wrote " + hostIpWrites + " host=>IP mappings")
	    
	    return (asWrites + ipAsWrites + hostIpWrites) > 0
  }
  
  def run() = {
    val json = Try(mapJson(Redis.getResolverResults.get).get).toOption
    if (json.isDefined) {
    	val imported = importResolverResults(json.get)
    	if (imported) {
    	  Redis.dropResolverResults()
    	  Logger.info("Importing resolver results complete")
    	} else {
    	  Logger.error("Importing resolver results failed!")
    	}
    }
  }
}