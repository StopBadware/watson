package controllers

import java.io.File
import scala.collection.JavaConversions._
import scala.io.Source
import scala.actors.Futures.future
import play.api._
import play.api.libs.json._
import play.api.mvc._
import models.Uri

object Rest extends Controller with JsonMapper {
  
	def timeoflast(source: String) = Action { request =>
		Ok(source) //TODO WTSN-20
  }
	
	def importList(importType: String, source: String) = Action(parse.temporaryFile) { request =>
	  Logger.info("Received " + importType + " for " + source)
	  val types = Set("blacklist", "clean", "appeals")
	  val sources = Set("goog", "nsf", "tts")
	  if (types.contains(importType) && sources.contains(source)) {
	  	future(processImport(Source.fromFile(request.body.file).mkString, source, importType))
	    Ok
	  } else {
	  	NotFound
	  }
  }
	
	private def processImport(json: String, source: String, importType: String) = {
	  Logger.debug("start\t"+System.currentTimeMillis/1000)	//DELME WTSN-11
	  mapJson(json).foreach { node =>
	    node.fieldNames.foreach { field =>
			  importType match {
			    case "blacklist" => {
			      val isDifferential = source match {
			        case "nsf" => false
			        case "tts" | "goog" => true
			      }
			      val blacklist = node.get(field).map(_.asText).toSet
		      	Blacklist.importBlacklist(Blacklist(blacklist, source, field.toLong, isDifferential))
			    }
			    case "clean" => 		//TODO WTSN-11 handle cleanlist
			    case "appeals" => 	//TODO WTSN-11 handle appeal results
			  }
	    }
	  }
	  Logger.debug("end\t"+System.currentTimeMillis/1000)	//DELME WTSN-11
	}
	
}