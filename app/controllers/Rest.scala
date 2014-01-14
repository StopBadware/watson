package controllers

import scala.io.Source
import scala.actors.Futures.future
import scala.util.Try
import play.api._
import play.api.mvc._
import models.{BlacklistEvent, Uri}

object Rest extends Controller with JsonMapper {
  
	def timeoflast(abbr: String) = Action { implicit request =>
		val source = models.Source.withAbbr(abbr)
		if (source.isDefined) {
		  val blTimeOfLast = BlacklistEvent.timeOfLast(source.get)
		  val redisTimeOfLast = Try(Redis.blacklistTimes(source.get).max).getOrElse(0L)
			Ok(Math.max(blTimeOfLast, redisTimeOfLast).toString)
		} else {
		  NotFound
		}
  }
	
	def importList(abbr: String) = Action(parse.temporaryFile) { implicit request =>
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
	
	def requestReview = Action { implicit request =>
	  request.body.asJson.map { json =>
	    println(json)	//DELME WTSN-30
	    val uri = Uri.findOrCreate(json.\("uri").toString)
	    println(uri)	//DELME WTSN-30
	  }
	  BadRequest	//TODO WTSN-30
	}
	
}