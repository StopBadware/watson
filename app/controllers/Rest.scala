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
	  Logger.debug("BEGIN PARSING")				//DELME
	  val str = Source.fromFile(new java.io.File("tmp/foo")).mkString	
	  Logger.debug(str.length.toString)		//DELME
//	  val json = Json.parse(str)
	  import com.fasterxml.jackson.core._
	  val mapper = new com.fasterxml.jackson.databind.ObjectMapper
//	  val jf = mapper.getJsonFactory
//	  val jp = jf.createJsonParser(new java.io.File("tmp/foo"))
	  val json = mapper.readTree(str)			//DELME
	  Logger.debug("PARSING COMPLETE")		//DELME
	  val foo = json.get("1367412595")
	  println(foo.getClass+"\t"+foo.size)
//	  val foo = (json \ "1367412595").asOpt[Array[String]]	//DELME
//	  Logger.debug("Defined: "+foo.isDefined)		//DELME
	  Logger.debug("SUBROUTINE END")		//DELME
		Ok("blacklist")
  }
	
	def cleanlist(source: String) = Action { request =>
		Ok("cleanlist")
  }
	
	def appeals(source: String) = Action { request =>
		Ok("appeals")
  }
}