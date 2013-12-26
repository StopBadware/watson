package controllers

import java.net.URISyntaxException
import scala.collection.JavaConversions._
import play.api.Logger
import play.api.mvc.Controller
import play.api.libs.json._
import com.fasterxml.jackson.databind.JsonNode
import com.codahale.jerkson.Json
import models._

object Blacklist extends Controller with JsonMapper {
  
  def importBlacklist(json: String, source: Source) = {
	  mapJson(json).foreach { node =>
      source match {
        case GOOG | TTS => addToQueue(node, source)
        case NSF => importNsfocus(node.toList)
        case _ => Logger.error("No import blacklist action for " + source)
	    }
	  }
  }
  
  def importGoogleAppeals(json: String) = {
    mapJson(json).foreach { nodes =>
      val rescans = nodes.toList
      Logger.info("Importing "+rescans.size+" Google rescans")
      val added = rescans.foldLeft(0) { (ctr, node) =>
        val url = node.get("url").asText
        val link = if (node.has("link")) Some(node.get("link").asText) else None
        val status = node.get("status").asText
        val requestedVia = node.get("source").asText
        val rescannedAt = node.get("time").asLong
        val uriId = Uri.findOrCreate(url)
        val relatedUriId = if (link.isDefined) {
          val related = Uri.findOrCreate(link.get)
          if (related.isDefined) Some(related.get.id) else None
        } else {
          None
        }
        val created = if (uriId.isDefined) {
          GoogleRescan.create(uriId.get.id, relatedUriId, status, requestedVia, rescannedAt)
        } else {
          false
        }
        if (created) ctr + 1 else ctr
      }
      Logger.info("Added "+added+" Google rescans")
    }
  }
  
  def importDifferential(reported: List[String], source: Source, time: Long): Boolean = {
    Logger.info("Importing "+reported.size+" entries for "+source)
    val uris = Uri.findOrCreateIds(Uri.asReportedUris(reported))
    Logger.info("Updating existing blacklist entries for "+source)
    val removed = updateNoLongerBlacklisted(uris, source, time)
    Logger.info("Marked "+removed+" URIs as no longer blacklisted by "+source)
    val endTime = if (BlacklistEvent.timeOfLast(source) > time) Some(time) else None
    val reportedEvents = uris.map(id => ReportedEvent(id, source, time, endTime))
    val addedOrUpdated = BlacklistEvent.createOrUpdate(reportedEvents, source)
    Logger.info("Imported "+addedOrUpdated+" blacklist events for "+source)
    return addedOrUpdated > 0
  }
  
  private def addToQueue(json: JsonNode, source: Source) = {
    diffBlacklist(json).foreach { case (time, blacklist) =>
      Logger.info("Queueing import from "+time+" for "+source)
      Redis.addBlacklist(source, time, blacklist)
      Logger.info("Queued import with "+blacklist.size+" entries for "+source)
    }
  }  
  
  private def diffBlacklist(blacklist: JsonNode): Map[Long, List[String]] = {
    return Json.parse[List[BlacklistEntry]](blacklist).groupBy(_.time)
			.foldLeft(Map.empty[Long, List[String]]) { case (map, (time, entries)) =>
      	map ++ Map(time -> entries.map(_.url))
    }
  }
  
//  private def updateNoLongerBlacklisted(blacklist: List[Uri], source: Source, time: Long): Int = {
//    def currentlyBlacklisted = BlacklistEvent.blacklisted(Some(source)).filter(_.blacklistedAt < time)
//    val old = currentlyBlacklisted
//    val newUriIds = blacklist.map(_.id)
//    old.filterNot(event => newUriIds.contains(event.uriId)).foreach(_.removeFromBlacklist(time))
//    return old.size - currentlyBlacklisted.size
//  } 
  private def updateNoLongerBlacklisted(blacklistIds: List[Int], source: Source, time: Long): Int = {
    def currentlyBlacklisted = BlacklistEvent.blacklisted(Some(source)).filter(_.blacklistedAt < time)
    val old = currentlyBlacklisted
    old.filterNot(event => blacklistIds.contains(event.uriId)).foreach(_.removeFromBlacklist(time))
    return old.size - currentlyBlacklisted.size
  }   
  
  private def importNsfocus(json: List[JsonNode], source: Source=Source.NSF) = {
  	Logger.info("Importing "+json.size+" entries for "+source)
  	val addedOrUpdated = json.foldLeft(0) { (ctr, node) =>
  		val url = node.get("url").asText
	    val time = node.get("time").asLong
	    val clean = node.get("clean").asLong
	    try {
	      val uri = Uri.findOrCreate(url)
	      val cleanTime = if (clean != 0) Some(clean) else None
	      if (uri.isDefined && uri.get.blacklist(source, time, cleanTime)) ctr + 1 else ctr
	    } catch {
	      case e: URISyntaxException => Logger.warn("URISyntaxException thrown, unable to create URI for '"+url+"': "+e.getMessage)
	      ctr
	    }
  	}
  	Logger.info("Added or updated "+addedOrUpdated+" blacklist events for "+source)
  }
  
}

case class Blacklist(source: Source, time: Long, urls: List[String]) {
  override def toString: String = "source=>"+source+" time=>"+time+" size=>"+urls.size
}
private case class BlacklistEntry(url: String, time: Long)