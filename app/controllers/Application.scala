package controllers

import play.api._
import play.api.mvc._
import com.fasterxml.jackson.databind.{JsonNode, JsonMappingException, ObjectMapper}

object Application extends Controller {
  
  def index = Action {
    Ok(views.html.index("Watson"))
  }
  
}

trait JsonMapper {
  
  def mapJson(txt: String): Option[JsonNode] = {
    return try {
	    val mapper = new ObjectMapper
	    Some(mapper.readTree(txt))
    } catch {
      case e:JsonMappingException => {
        Logger.error("JsonMappingException thrown parsing JSON:"+e.getMessage)
        None
      }
    }
  }
  
}