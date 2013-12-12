package controllers

import java.net.URISyntaxException
import scala.collection.JavaConversions._
import play.api.Logger
import play.api.mvc.Controller
import play.api.libs.json._
import com.fasterxml.jackson.databind.JsonNode
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
        val uriId = Uri.findOrCreate(new ReportedUri(url)).get.id
        val relatedUriId = if (link.isDefined) {
          Some(Uri.findOrCreate(new ReportedUri(link.get)).get.id)
        } else {
          None
        }
        if (GoogleRescan.create(uriId, relatedUriId, status, requestedVia, rescannedAt)) ctr + 1 else ctr
      }
      Logger.info("Added "+added+" Google rescans")
    }
  }
  
  def importDifferential(reported: List[ReportedUri], source: Source, time: Long) = {
    Logger.info("Importing "+reported.size+" entries for "+source)
    val uris = reported.map(Uri.findOrCreate(_)).flatten
    val removed = updateNoLongerBlacklisted(uris, source, time)
    Logger.info("Marked "+removed+" events as no longer blacklisted by "+source)
    val moreRecent = BlacklistEvent.blacklisted(Some(source)).filter(_.blacklistedAt > time).map(_.id)
    val addedOrUpdated = uris.foldLeft(0) { (ctr, uri) =>
      val endTime = if (moreRecent.isEmpty || moreRecent.contains(uri.id)) None else Some(time)
      if (uri.blacklist(source, time, endTime)) ctr + 1 else ctr
    }
    Logger.info("Added or updated "+addedOrUpdated+" blacklist events for "+source)
  }  
  
  def importDifferentials(json: List[JsonNode], source: Source) = {
    //TODO WTSN-39 pull from redis
    Logger.info("Queuing blacklist(s) for "+source)
    diffBlacklist(json).groupBy(_._2).foreach { case (time, blacklist) =>
      val uris = blacklist.map(_._1)
      importDifferential(uris, source, time)
    }
  }
  
  private def addToQueue(json: JsonNode, source: Source) = {
    Logger.info("Adding import for "+source+" to queue")
    //TODO WTSN-39 add to import queue (uris, source, time)
    Redis.addToMap("delkey", "delfield", json.toString)
    Logger.info("Added import for "+source+" to queue")
  }
  
  private def updateNoLongerBlacklisted(blacklist: List[Uri], source: Source, time: Long): Int = {
    def currentlyBlacklisted = BlacklistEvent.blacklisted(Some(source)).filter(_.blacklistedAt < time)
    val old = currentlyBlacklisted
    val newUriIds = blacklist.map(_.id)
    old.filterNot(event => newUriIds.contains(event.uriId)).foreach(_.removeFromBlacklist(time))
    return old.size - currentlyBlacklisted.size
  }
  
  private def diffBlacklist(blacklist: List[JsonNode]): List[(ReportedUri, Long)] = {
    return blacklist.foldLeft(List.empty[(ReportedUri, Long)]) { (list, node) =>
	    val url = node.get("url").asText
	    val time = node.get("time").asLong
      try {
        list ++ List((new ReportedUri(url), time))
      } catch {
        case e: URISyntaxException => Logger.warn("URISyntaxException thrown, unable to create URI for '"+url+"': "+e.getMessage)
        list
      }
    }
  }
  
  private def importNsfocus(json: List[JsonNode], source: Source=Source.NSF) = {
  	Logger.info("Importing "+json.size+" entries for "+source)
  	val addedOrUpdated = json.foldLeft(0) { (ctr, node) =>
  		val url = node.get("url").asText
	    val time = node.get("time").asLong
	    val clean = node.get("clean").asLong
	    try {
	      val uri = Uri.findOrCreate(new ReportedUri(url)).get
	      val cleanTime = if (clean != 0) Some(clean) else None
	      if (uri.blacklist(source, time, cleanTime)) ctr + 1 else ctr
	    } catch {
	      case e: URISyntaxException => Logger.warn("URISyntaxException thrown, unable to create URI for '"+url+"': "+e.getMessage)
	      ctr
	    }
  	}
  	Logger.info("Added or updated "+addedOrUpdated+" blacklist events for "+source)
  }

}