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
        case GOOG | TTS => importDifferential(node.toList, source)
        case NSF => importNsfocus(node.toList)
        case _ => Logger.error("No import blacklist action for " + source)
	    }
	  }
  }
  
  def importGoogleAppeals(json: String) = {
    println("TODO WTSN-11 GOOGAPL HANDLING")	//TODO WTSN-11
  }
  
  private def importDifferential(json: List[JsonNode], source: Source) = {
    Logger.info("Importing "+json.size+" entries for "+source)
    diffBlacklist(json).groupBy(_._2).foreach { case (time, blacklist) =>
      val uris = blacklist.map(_._1)
      val removed = updateNoLongerBlacklisted(uris, source, time)
      Logger.info("Marked "+removed+" events as no longer blacklisted by "+source)
      val addedOrUpdated = uris.foldLeft(0) { (ctr, uri) => 
        if (uri.blacklist(source, time)) ctr + 1 else ctr
      }
      Logger.info("Added or updated "+addedOrUpdated+" blacklist events for "+source)
    }
  }
  
  private def updateNoLongerBlacklisted(blacklist: List[Uri], source: Source, time: Long): Int = {
    def currentlyBlacklisted = BlacklistEvent.blacklisted(Some(source)).filter(_.blacklistedAt < time)
    val old = currentlyBlacklisted
    val newUriIds = blacklist.map(_.id)
    old.filterNot(event => newUriIds.contains(event.uriId)).foreach(_.removeFromBlacklist(time))
    return old.size - currentlyBlacklisted.size
  }
  
  private def diffBlacklist(blacklist: List[JsonNode]): List[(Uri, Long)] = {
    return blacklist.foldLeft(List.empty[(Uri, Long)]) { (list, node) =>
	    val url = node.get("url").asText
	    val time = node.get("time").asLong
      try {
        list ++ List((Uri.findOrCreate(new ReportedUri(url)).get, time))
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