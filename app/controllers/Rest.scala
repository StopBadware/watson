package controllers

import scala.collection.immutable.{HashMap, HashSet}
import scala.io.Source
import play.api._
import play.api.libs.json._
import play.api.mvc._

object Rest extends Controller {

	def timeoflast(source: String) = Action { request =>
		Ok(views.html.index("timeoflast for "+source))	//TODO: WTSN-20
  }
	
	def blacklist(source: String) = Action(parse.json) { request =>
	  Logger.debug("IN SUBROUTINE")		//DELME
//	  val str = Source.fromFile(request.body.file).mkString	//parse.temporaryFile
	  val json = request.body
//	  Logger.debug(str.length.toString)	//DELME
//	  val json = Json.parse(str)
//	  val json = Json.toJson(str)
//	  val json = com.codahale.jerkson.Json.parse(request.body.file)	//TODO: FIXME
	  Logger.debug("PARSING COMPLETE")		//DELME
	  val foo = (json \ "1367412595").asOpt[Array[String]]	//DELME
	  Logger.debug("Defined: "+foo.isDefined)		//DELME
//	  Logger.info(foo.isDefined.toString)	//DELME
		Ok("blacklist")
  }
	
	def cleanlist(source: String) = Action { request =>
		Ok("cleanlist")
  }
	
	def appeals(source: String) = Action { request =>
		Ok("appeals")
  }
}