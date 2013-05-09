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
	
	def blacklist(source: String) = Action(parse.temporaryFile) { request =>
	  val str = Source.fromFile(request.body.file).mkString
	  Logger.info(str.length.toString)	//DELME
	  val json = Json.parse(str)
	  Logger.info("PARSING COMPLETE")		//DELME
//	  val foo = (json \ "1367412595").asOpt[Array[String]]
//	  println(foo.isDefined)	//DELME
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