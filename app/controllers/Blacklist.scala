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
	  println("start\t"+System.currentTimeMillis/1000)	//DELME WTSN-11
	  mapJson(json).foreach { node =>
      source match {
        case GOOG | TTS => importDifferential(node.toList, source)
        case NSF => importNsfocus(node.toList)
        case _ => Logger.error("No import blacklist action for " + source)
	    }
	  }
	  println("end\t"+System.currentTimeMillis/1000)	//DELME WTSN-11    
  }
  
  def importGoogleAppeals(json: String) = {
    println("TODO WTSN-11 GOOGAPL HANDLING")	//TODO WTSN-11
  }
  
  private def importDifferential(json: List[JsonNode], source: Source) = {
    Logger.info("Importing "+json.size+" entries for "+source)
    diffBlacklist(json).groupBy(_._2).foreach { case (time, blacklist) =>
      val uris = blacklist.map(_._1)
      dropNoLongerBlacklisted(uris, source, time)
      //TODO WTSN-11 add/update blacklist
      //.foreach(_.blacklist(source, time))
    }
  }
  
  private def dropNoLongerBlacklisted(blacklist: List[Uri], source: Source, time: Long) = {
    //TODO WTSN-11 get all currently blacklisted by source
    //TODO WTSN-11 compare to blacklist
    //TODO WTSN-11 any missing from blacklist remove with time
    println(blacklist)	//DELME WTSN-11
//    val current = BlacklistEvent.
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
  
  private def importNsfocus(blacklist: List[JsonNode]) = {
  	println("TODO WTSN-11 NSF HANDLING")			//TODO WTSN-11
  }

}