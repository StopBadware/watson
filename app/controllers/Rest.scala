package controllers

import scala.io.Source
import scala.actors.Futures.future
import play.api._
import play.api.mvc._
import models.BlacklistEvent

object Rest extends Controller with JsonMapper {
  
	def timeoflast(abbr: String) = Action { request =>
		val source = models.Source.withAbbr(abbr)
		if (source.isDefined) {
			Ok(BlacklistEvent.timeOfLast(source.get).toString)
		} else {
		  NotFound
		}
  }
	
	def importList(abbr: String) = Action(parse.temporaryFile) { request =>
	  Logger.info("Received import for " + abbr)
	  val source = models.Source.withAbbr(abbr)
	  if (source.isDefined) {
	    future(Blacklist.importBlacklist(Source.fromFile(request.body.file).mkString, source.get))
	    Ok
	  } else if (abbr.equalsIgnoreCase("googapl")) {
	    future(Blacklist.importGoogleAppeals(Source.fromFile(request.body.file).mkString))
	    Ok
	  } else {
	  	NotFound
	  }
  }
	
}