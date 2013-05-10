package controllers

import scala.collection.immutable.{HashMap, HashSet}
import scala.collection.JavaConversions._
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
	  val str = Source.fromFile(request.body.file).mkString	
	  val mapper = new ObjectMapper
	  val json = mapper.readTree(str)	
	  val blTimes = json.fieldNames.toList
	  blTimes.foreach { bltime =>
	    val blacklist = json.get(bltime).iterator.toList
	    blacklist.foreach { entry =>
	      //TODO: store entries in db
	      println(entry)		//DELME
	    }
	  }
		Ok
  }
	
	def cleanlist(source: String) = Action { request =>
		Ok("cleanlist")
  }
	
	def appeals(source: String) = Action { request =>
		Ok("appeals")
  }
}