package controllers

import scala.io.Source
import scala.actors.Futures.future
import play.api._
import play.api.mvc._

object Rest extends Controller with JsonMapper {
  
	def timeoflast(source: String) = Action { request =>
		Ok(source) //TODO WTSN-20
  }
	
	def importList(source: String) = Action(parse.temporaryFile) { request =>
	  Logger.info("Received import for " + source)
	  val sources = Set("goog", "googapl", "nsf", "tts")
	  if (sources.contains(source)) {
	    future(Blacklist.importBlacklist(Source.fromFile(request.body.file).mkString, source))
	    Ok
	  } else {
	  	NotFound
	  }
  }
	
}