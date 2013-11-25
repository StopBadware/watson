package controllers

import java.net.URISyntaxException
import scala.collection.JavaConversions._
import play.api.Logger
import play.api.mvc.Controller
import play.api.libs.json._
import com.fasterxml.jackson.databind.JsonNode
import models._

object Blacklist extends Controller with JsonMapper {
  
  def importBlacklist(json: String, source: String) {
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
    val urls = blacklist.foldLeft(Set.empty[(String, Long)]) { (set, node) =>
      set + ((node.get("url").toString, node.get("time").asLong))
    }
    println(urls.size, urls)	//DELME WTSN-11
  }
  
  private def importGoogleAppeals(appealResults: List[JsonNode]) = {
    println("TODO WTSN-11 GOOGAPL HANDLING")	//TODO WTSN-11
  }
  
  private def importNsfocus(blacklist: List[JsonNode]) = {
  	println("TODO WTSN-11 NSF HANDLING")			//TODO WTSN-11
  }

}