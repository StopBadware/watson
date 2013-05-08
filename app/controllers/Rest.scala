package controllers

import scala.collection.immutable.{HashMap, HashSet}
import play.api._
import play.api.libs.json._
import play.api.mvc._

object Rest extends Controller {

	def timeoflast(source: String) = Action { request =>
		Ok(views.html.index("timeoflast for "+source))	//TODO: WTSN-20
  }
	
	def blacklist(source: String) = Action { request =>
	  println(request.body.asJson)	//DELME
	  val json = (request.body.asJson).get
//	  val foo = (json \ "object").asOpt[Map[String, String]]
//	  println(foo.getOrElse("not provided"))
//	  println(foo.get("a"))
		Ok("blacklist")
  }
	
	def cleanlist(source: String) = Action { request =>
		Ok("cleanlist")
  }
	
	def appeals(source: String) = Action { request =>
		Ok("appeals")
  }
}