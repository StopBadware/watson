package controllers

import java.io.File
import scala.io.Source
import play.api._
import play.api.libs.json._
import play.api.mvc._
import models.Uri

object Rest extends Controller {
  
	def timeoflast(source: String) = Action { request =>
	  Logger.debug("calling bg controller...")	//DELME WTSN-11	
	  controllers.Background.foo	//DELME WTSN-11
	  Logger.debug("...bg controller called")	//DELME WTSN-11
//		Ok(views.html.index("timeoflast for "+source))	//TODO WTSN-20
		Ok(source)
  }
	
	def blacklist(source: String) = Action(parse.temporaryFile) { request =>
	  Logger.info("Received blacklist for " + source)
	  addToQueue(Source.fromFile(request.body.file).mkString, source)
		Ok	//TODO WTSN-11 return 200 or 500
  }
	
	def cleanlist(source: String) = Action(parse.temporaryFile) { request =>
	  Logger.info("Received blacklist for " + source)
	  addToQueue(Source.fromFile(request.body.file).mkString, source, "cleanlist")
		Ok //TODO WTSN-11 return 200 or 500
  }
	
	def appeals(source: String) = Action(parse.temporaryFile) { request =>
	  Logger.info("Received appeal results for " + source)
	  addToQueue(Source.fromFile(request.body.file).mkString, source, "appealresults")
		Ok //TODO WTSN-11 return 200 or 500
  }
	
	private def addToQueue(json: String, source: String, importType: String="blacklist"): Boolean = {
	  Logger.debug(json.length+"\t"+System.currentTimeMillis/1000)	//DELME WTSN-11
//	  return Redis.importQueuePush(source, importType, json)
	  return true	//DELME
	}
	
}