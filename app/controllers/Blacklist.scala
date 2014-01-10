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
    Logger.debug("FINDING OR CREATING URIS\t"+Runtime.getRuntime.freeMemory)	//DELME WTSN-46
    val uris = Uri.findOrCreateIds(reported)
    Logger.debug("MAPPING URIS->EVENTS\t"+Runtime.getRuntime.freeMemory)	//DELME WTSN-46
    val urisEvents = BlacklistEvent.blacklistedUriIdsEventIds(time, Some(source))
    Logger.debug("UNBLACKLISTING EVENTS\t"+Runtime.getRuntime.freeMemory)	//DELME WTSN-46
    val toRemove = urisEvents.filterNot(ids => uris.contains(ids._1))
    val removed = BlacklistEvent.unBlacklist(toRemove.values.toSet, time)
    Logger.info("Marked "+removed+" URIs as no longer blacklisted by "+source)
    Logger.debug("UPDATING EXISTING EVENTS\t"+Runtime.getRuntime.freeMemory)	//DELME WTSN-46
    val timeOfLast = BlacklistEvent.timeOfLast(source)
    val updated = if (time < timeOfLast) {
      val toUpdate = urisEvents.filter(ids => uris.contains(ids._1))
      val updCount = BlacklistEvent.update(toUpdate.values.toSet, time)
    	Logger.info("Updated "+updCount+" existing blacklist events for "+source)
    	updCount 
    } else {
      0
    }
    Logger.debug("ADDING NEW EVENTS\t"+Runtime.getRuntime.freeMemory)	//DELME WTSN-46
    val endTime = if (time >= timeOfLast) None else Some(time)
    val toCreate = uris.filterNot(urisEvents.contains(_))
    val created = BlacklistEvent.create(toCreate, source, time, endTime)
    Logger.info("Added "+created+" new blacklist events for "+source)
    Logger.info("Imported "+(updated+created)+" blacklist events for "+source)
    return (updated+created) > 0
  }  
  
  private def addToQueue(json: JsonNode, source: Source) = {
    val time = json.get("time").asLong
    val blacklist = Json.parse[List[String]](json.get("blacklist"))
    Logger.info("Queueing import from "+time+" for "+source)
    Redis.addBlacklist(source, time, blacklist)
    Logger.info("Queued import with "+blacklist.size+" entries for "+source)
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