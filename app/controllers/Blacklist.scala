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
  
  def importDifferential(reported: List[String], source: Source, time: Long) = {
    Logger.info("Importing "+reported.size+" entries for "+source)
    val uris = reported.map(u => Uri.findOrCreate(new ReportedUri(u))).flatten	//TODO WTSN-39 handle URI exception
    val removed = updateNoLongerBlacklisted(uris, source, time)
    Logger.info("Marked "+removed+" events as no longer blacklisted by "+source)
    val moreRecent = BlacklistEvent.blacklisted(Some(source)).filter(_.blacklistedAt > time).map(_.id)
    val addedOrUpdated = uris.foldLeft(0) { (ctr, uri) =>
      val endTime = if (moreRecent.isEmpty || moreRecent.contains(uri.id)) None else Some(time)
      if (uri.blacklist(source, time, endTime)) ctr + 1 else ctr
    }
    Logger.info("Added or updated "+addedOrUpdated+" blacklist events for "+source)
  }   
  
  private def addToQueue(json: JsonNode, source: Source) = {
    diffBlacklist(json).foreach { case (time, blacklist) =>
      Logger.info("Adding import from "+time+" for "+source+" to queue")
      Redis.addBlacklist(source, time, blacklist)
      Logger.info("Added import with "+blacklist.size+" entries for "+source+" to queue")
    }
  }  
  
  private def diffBlacklist(blacklist: JsonNode): Map[Long, List[String]] = {
    return Json.parse[List[BlacklistEntry]](blacklist).groupBy(_.time)
  						.foldLeft(Map.empty[Long, List[String]]) { case (map, (time, entries)) =>
      map ++ Map(time -> entries.map(_.url))
    }
  }
  
  private def updateNoLongerBlacklisted(blacklist: List[Uri], source: Source, time: Long): Int = {
    def currentlyBlacklisted = BlacklistEvent.blacklisted(Some(source)).filter(_.blacklistedAt < time)
    val old = currentlyBlacklisted
    val newUriIds = blacklist.map(_.id)
    old.filterNot(event => newUriIds.contains(event.uriId)).foreach(_.removeFromBlacklist(time))
    return old.size - currentlyBlacklisted.size
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
  
  private case class BlacklistEntry(url: String, time: Long)

}