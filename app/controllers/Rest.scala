package controllers

import scala.collection.immutable.{HashMap, HashSet}
import scala.collection.JavaConversions._ 	//DELME?
import scala.io.Source
import play.api._
import play.api.libs.json._
import play.api.mvc._
import com.fasterxml.jackson.databind.ObjectMapper

object Rest extends Controller {

	def timeoflast(source: String) = Action { request =>
		Ok(views.html.index("timeoflast for "+source))	//TODO: WTSN-20
  }
	
	def blacklist(source: String) = Action(parse.temporaryFile) { request =>
	  Logger.debug("SUBROUTINE BEGIN")	//DELME
	  val str = Source.fromFile(request.body.file).mkString	
	  Logger.debug(str.length.toString)	//DELME
	  Logger.debug("PARSING BEGIN")			//DELME
	  val mapper = new ObjectMapper
	  val json = mapper.readTree(str)	
	  Logger.debug("PARSING COMPLETE")	//DELME
//	  val foo = json.get("1367412595")
//	  println(foo.getClass+"\t"+foo.size)
//	  val foo = json.get("1367412595")
	  val foo = json.fieldNames.toList
	  foo.foreach(f => println(f))
//	  val foo = json.path(1)
//	  println(foo.size())
//	  println(foo.get(0))
//	  println(foo.path(0).toString())
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