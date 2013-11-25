package controllers

import java.net.URISyntaxException
import scala.collection.JavaConversions._
import play.api.Logger
import play.api.mvc.Controller
import play.api.libs.json._
import models._

object Blacklist extends Controller with JsonMapper{
  
//  def OLDimportBlacklist(blist: Blacklist) {	//DELME WTSN-11
//    println("size: "+blist.uris.size,"source: "+blist.source,"time: "+blist.blTime,"isDiff: "+blist.isDifferential) //DELME WTSN-11
//    blist.uris.foreach { rawUri =>
//	    try {
//	    	new ReportedUri(rawUri).blacklist(blist.source, blist.blTime)
////	    	val uri = Uri(rawUri)	//DELME WTSN-11
////    		println(uri.uri,uri.hierarchicalPart,uri.path,uri.query,uri.reversedHost,uri.sha2)	//DELME WTSN-11	
//	    } catch {
//	      case e: URISyntaxException => Logger.warn("Unable to parse a valid URI from "+rawUri+"\t"+e.getMessage)
//	    }
//    }
//    
//    if (blist.isDifferential) {
//    	//TODO WTSN-11 update entries for source not in blist with bltimes < bl.time
//    }
//    
//  }
  
  def importBlacklist(json: String, source: String) {
	  Logger.debug("start\t"+System.currentTimeMillis/1000)	//DELME WTSN-11
	  mapJson(json).foreach { node =>
	    node.fieldNames.foreach { field =>
	      println(field)	//DELME
	      source match {
	        case "goog" | "tts" => println("TODO WTSN-11 DIFF BLACKLIST")
	        case "googapl" => println("TODO WTSN-11 GOOGAPL HANDLING")	//TODO WTSN-11
	        case "nsf" => println("TODO WTSN-11 NSF HANDLING")					//TODO WTSN-11
	      }
	      println(field)	//DELME WTSN-11
//	      val blacklist = node.get(field).map(_.asText).toSet
//      	Blacklist.importBlacklist(Blacklist(blacklist, source, field.toLong, isDifferential))
	    }
	  }
	  Logger.debug("end\t"+System.currentTimeMillis/1000)	//DELME WTSN-11    
  }
  
  private def importDifferential(source: String) = {
    //TODO WTSN-11
  }
  
  private def importGoogleAppeals() = {
    //TODO WTSN-11
  }
  
  private def importNsfocus() = {
    //TODO WTSN-11
  }

}