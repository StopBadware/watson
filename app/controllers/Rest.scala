package controllers

import java.io.File
import scala.collection.immutable.{HashMap, HashSet}
import scala.collection.JavaConversions._
import scala.io.Source
import play.api._
import play.api.libs.json._
import play.api.mvc._
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import models.Uri

object Rest extends Controller {
  
  def getJson(file: File): JsonNode = {
    val str = Source.fromFile(file).mkString
    val mapper = new ObjectMapper
    return mapper.readTree(str)
  }

	def timeoflast(source: String) = Action { request =>
		Ok(views.html.index("timeoflast for "+source))	//TODO: WTSN-20
  }
	
	def blacklist(source: String) = Action(parse.temporaryFile) { request =>
	  val json = getJson(request.body.file)
	  val blTimes = json.fieldNames.toList
	  blTimes.foreach { bltime =>
	    val blacklist = json.get(bltime).iterator.toList
	    blacklist.foreach { entry =>
	      Uri.add(entry.asText, source, bltime.toLong)
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