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
	  val validTypes = Set("blacklist", "clean", "appeals")
	  if (validTypes.contains(importType)) {
	  	future(proccessImport(Source.fromFile(request.body.file).mkString, source, importType))
	    Ok
	  } else {
	  	NotFound
	  }
  }
	
	private def proccessImport(json: String, source: String, importType: String) = {
	  Logger.debug(json.length+"\t"+System.currentTimeMillis/1000)	//DELME WTSN-11
	  val baz = mapJson(json).get							//DELME
	  println(baz.getClass)										//DELME
	  val bazStr = baz.toString								//DELME
	  println(bazStr.substring(0, math.min(bazStr.length, 150)))				//DELME
	  println(baz.fieldNames.toList)					//DELME
	}
	
}