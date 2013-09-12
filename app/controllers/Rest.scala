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
	
	def blacklist(source: String) = Action(parse.temporaryFile) { request =>
	  Logger.info("Received blacklist for " + source)
	  future(proccessImport(Source.fromFile(request.body.file).mkString, source))
		Ok
  }
	
	def cleanlist(source: String) = Action(parse.temporaryFile) { request =>
	  Logger.info("Received blacklist for " + source)
	  future(proccessImport(Source.fromFile(request.body.file).mkString, source, "cleanlist"))
		Ok
  }
	
	def appeals(source: String) = Action(parse.temporaryFile) { request =>
	  Logger.info("Received appeal results for " + source)
	  future(proccessImport(Source.fromFile(request.body.file).mkString, source, "appealresults"))
		Ok
  }
	
	private def proccessImport(json: String, source: String, importType: String="blacklist") = {
	  Logger.debug(json.length+"\t"+System.currentTimeMillis/1000)	//DELME WTSN-11
	  val foo = Redis.importQueueGet("goog1379001958146blacklist").getOrElse("")	//DELME SMALL
//	  val foo = Redis.importQueueGet("goog1379002972344blacklist").getOrElse("")	//DELME FULL
	  val baz = mapJson(foo).get							//DELME
	  println(baz.getClass)										//DELME
	  println(baz.toString.substring(0, 100))	//DELME
	  println(baz.fieldNames.toList)					//DELME
	}
	
}