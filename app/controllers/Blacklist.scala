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
  
  private def importDifferential(blacklist: List[JsonNode], source: Source) = {
    println("TODO WTSN-11 DIFF BLACKLIST")	//DELME WTSN-11
    blacklist.foreach { node =>
//      val url = node.get("url").asText
//      val time = node.get("time").asLong
//      println(url, time)	//DELME
//      try {
//        Uri.create(new ReportedUri(url))		//TODO WTSN-11 return Uri and call blacklist w/time and source
//      } catch {
////        case e: URISyntaxException => Logger.warn("URISyntaxException thrown, unable to create URI for '"+url+"': "+e.getMessage)
////        case e: Exception => Logger.warn("Unable to create URI for '"+url+"': "+e.getMessage)
//        case e: URISyntaxException => println("URISyntaxException thrown, unable to create URI for '"+url+"': "+e.getMessage)
//        case e: Exception => println("Unable to create URI for '"+url+"': "+e.getMessage)
//      }
    }
  }
  
  private def importNsfocus(blacklist: List[JsonNode]) = {
  	println("TODO WTSN-11 NSF HANDLING")			//TODO WTSN-11
  }

}