package controllers

import scala.collection.JavaConversions._
import scala.actors.Futures.future
import play.api.Logger
import play.api.mvc.Controller
import com.fasterxml.jackson.databind.{JsonNode, JsonMappingException, ObjectMapper}

object Background extends Controller {
  
  def foo {
    Logger.debug("before the future")	//DELME WTSN-11
    future {
      Logger.debug("future i am in you")	//DELME WTSN-11
    	Logger.info("about to sleep")	//DELME WTSN-11
    	Thread.sleep(50000)	//DELME WTSN-11
    	Logger.debug("yawn, wakie wakie")	//DELME WTSN-11
    }
    Logger.debug("after the future")	//DELME WTSN-11
  }
  
  def importPendingQueue {
    //TODO WTSN-11
	  Logger.debug(Redis.importQueueKeys.toString)	//DELME
	  val foo = Redis.importQueueGet("goog1379001958146blacklist").getOrElse("")	//DELME SMALL
//	  val foo = Redis.importQueueGet("goog1379002972344blacklist").getOrElse("")	//DELME FULL
	  val baz = mapJson(foo).get							//DELME
	  println(baz.getClass)										//DELME
	  println(baz.toString.substring(0, 100))	//DELME
	  println(baz.fieldNames.toList)					//DELME
  }
  
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