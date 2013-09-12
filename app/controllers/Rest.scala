package controllers

import java.io.File
import scala.collection.JavaConversions._
import scala.io.Source
import play.api._
import play.api.libs.json._
import play.api.mvc._
import com.fasterxml.jackson.databind.{JsonNode, JsonMappingException, ObjectMapper}
import models.Uri

object Rest extends Controller {
  
  def mapJson(raw: String): Option[JsonNode] = {
    return try {
	    val mapper = new ObjectMapper
	    Some(mapper.readTree(raw))
    } catch {
      case e:JsonMappingException => {
        Logger.error("JsonMappingException thrown parsing JSON:"+e.getMessage)
        None
      }
    }
  }  

	def timeoflast(source: String) = Action { request =>
		Ok(views.html.index("timeoflast for "+source))	//TODO WTSN-20
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
	
	private def addToQueue(json: String, source: String, importType: String="blacklist") {
	  Logger.debug(Redis.importQueueKeys.toString)
	  Redis.importQueuePush(source, importType, json.toString)
	  val foo = Redis.importQueueGet("goog1379001958146blacklist").getOrElse("")	//DELME SMALL
//	  val foo = Redis.importQueueGet("goog1379002972344blacklist").getOrElse("")	//DELME FULL
	  val baz = mapJson(foo).get							//DELME
	  println(baz.getClass)										//DELME
	  println(baz.toString.substring(0, 100))	//DELME
	  println(baz.fieldNames.toList)					//DELME
	  Logger.debug(json.length+"\t"+System.currentTimeMillis/1000)	//DELME WTSN-11
	}
	
}