package controllers

import play.api._
import play.api.mvc._
import com.fasterxml.jackson.databind.{JsonNode, JsonMappingException, ObjectMapper}
import com.fasterxml.jackson.core.JsonParseException

object Application extends Controller {
  
  def index = Action {
    Ok(views.html.index("Watson"))
  }
  
}

trait JsonMapper {
  
  def mapJson(txt: String): Option[JsonNode] = {
    return try {
	    Some((new ObjectMapper).readTree(txt))
    } catch {
      case e: Exception => Logger.error("Exception thrown parsing JSON: " + e.getMessage)
      None
    }
  }
  
}