package controllers

import java.net.URISyntaxException
import scala.collection.JavaConversions._
import play.api.Logger
import play.api.mvc.Controller
import play.api.libs.json._
import com.fasterxml.jackson.databind.JsonNode
import models._

object Blacklist extends Controller with JsonMapper {
  
  def importBlacklist(json: String, source: String) = {
	  println("start\t"+System.currentTimeMillis/1000)	//DELME WTSN-11
	  mapJson(json).foreach { node =>
      source match {
        case "goog" | "tts" => importDifferential(node.toList, source)
        case "googapl" => importGoogleAppeals(node.toList)
        case "nsf" => importNsfocus(node.toList)
	    }
	  }
	  println("end\t"+System.currentTimeMillis/1000)	//DELME WTSN-11    
  }
  
  private def importDifferential(blacklist: List[JsonNode], source: String) = {
    println("TODO WTSN-11 DIFF BLACKLIST")	//DELME WTSN-11
    blacklist.foreach { node =>
      val url = node.get("url").asText
      val time = node.get("time").asLong
      println(url, time)	//DELME
      try {
        Uri.create(new ReportedUri(url))		//TODO WTSN-11 return Uri and call blacklist w/time and source
      } catch {
//        case e: URISyntaxException => Logger.warn("URISyntaxException thrown, unable to create URI for '"+url+"': "+e.getMessage)
//        case e: Exception => Logger.warn("Unable to create URI for '"+url+"': "+e.getMessage)
        case e: URISyntaxException => println("URISyntaxException thrown, unable to create URI for '"+url+"': "+e.getMessage)
        case e: Exception => println("Unable to create URI for '"+url+"': "+e.getMessage)
      }
    }
  }
  
  private def importGoogleAppeals(appealResults: List[JsonNode]) = {
    println("TODO WTSN-11 GOOGAPL HANDLING")	//TODO WTSN-11
  }
  
  private def importNsfocus(blacklist: List[JsonNode]) = {
  	println("TODO WTSN-11 NSF HANDLING")			//TODO WTSN-11
  }

}