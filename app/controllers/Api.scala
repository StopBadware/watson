package controllers

import scala.actors.Futures.future
import scala.util.Try
import scala.io.{Source => IoSource}
import play.api._
import play.api.mvc._
import models.{BlacklistEvent, Uri}
import models.enums.Source

object Api extends Controller with JsonMapper {
  
	def timeoflast(abbr: String) = Action { implicit request =>
		val source = Source.withAbbr(abbr)
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
	  val source = Source.withAbbr(abbr)
	  if (source.isDefined) {
	    future(Blacklist.importBlacklist(IoSource.fromFile(request.body.file).mkString, source.get))
	    Ok
	  } else if (abbr.equalsIgnoreCase("googapl")) {
	    future(Blacklist.importGoogleAppeals(IoSource.fromFile(request.body.file).mkString))
	    Ok
	  } else {
	  	NotFound
	  }
  }
	
	def requestReview = Action { implicit request =>
	  val body = Try(mapJson(request.body.asJson.get.toString).get)
	  if (body.isSuccess) {
	    val json = body.get
	    try {
		    val uri = Uri.findOrCreate(json.get("uri").asText).get
		    val email = json.get("email").asText
		    val ip = if (json.has("ip")) Some(json.get("ip").asLong) else None
		    val notes = if (json.has("notes")) Some(json.get("notes").asText) else None
	    	if (uri.requestReview(email, ip, notes)) Ok else UnprocessableEntity
	    } catch {
	      case _: Exception => BadRequest
	    }
	  } else {
	  	UnsupportedMediaType
	  }
	}
	
}