package controllers

import java.io.File
import scala.collection.immutable.{HashMap, HashSet}
import scala.collection.JavaConversions._
import scala.io.Source
import play.api._
import play.api.libs.json._
import play.api.mvc._
import com.fasterxml.jackson.databind.{JsonNode, JsonMappingException, ObjectMapper}
import models.Uri

object Rest extends Controller {
  
  def getJson(file: File): Option[JsonNode] = {
    try {
	    val str = Source.fromFile(file).mkString
	    val mapper = new ObjectMapper
	    return Option(mapper.readTree(str))
    } catch {
      case e:JsonMappingException => {
        Logger.error("JsonMappingException thrown parsing JSON:"+e.getMessage)
        return None
      }
    }
  }

	def timeoflast(source: String) = Action { request =>
		Ok(views.html.index("timeoflast for "+source))	//TODO: WTSN-20
  }
	
	def diffblacklist(source: String) = Action(parse.temporaryFile) { request =>
	  val json = getJson(request.body.file)
	  if (json.isDefined) {
	  	processList(json.get, source, true, true)
	  }
		Ok
  }	
	
	def blacklist(source: String) = Action(parse.temporaryFile) { request =>
	  val json = getJson(request.body.file)
	  if (json.isDefined) {
	  	processList(json.get, source, true, false)
	  }
		Ok
  }
	
	def cleanlist(source: String) = Action(parse.temporaryFile) { request =>
	  val json = getJson(request.body.file)
	  if (json.isDefined) {
	  	processList(json.get, source, false, false)
	  }
		Ok
  }
	
	private def processList(json: JsonNode, source: String, isBlacklist: Boolean, isDiffBL: Boolean) = {
	  val blTimes = json.fieldNames.toList
	  blTimes.foreach { bltime =>
	    val blacklist = json.get(bltime).iterator.toList
	    if (isDiffBL) {
  	    processDiff(blacklist, source, bltime.toLong)	//TODO move long check
  	  }	
	    blacklist.foreach { entry =>
	      try {
	        if (isBlacklist) {
	        	Uri.blacklisted(entry.asText, source, bltime.toLong)
	        } else {
	          Uri.removeFromBlacklist(entry.asText, source, bltime.toLong)
	        }
	      } catch {
	        case e:Exception => Logger.error("Unable to add "+entry.toString+":\t"+e.getClass+" - "+e.getMessage)
	      }
	    }
	  }
	}
	
	private def processDiff(newList: List[JsonNode], source: String, removedTime: Long) = {
	  //TODO: WTSN-11 handle diff bl
  	println("process diff bl")	//DELME
	}
	
	def appeals(source: String) = Action { request =>
		Ok("appeals")
  }
}