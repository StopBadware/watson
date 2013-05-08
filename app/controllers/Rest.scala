package controllers

import play.api._
import play.api.mvc._

object Rest extends Controller {

	def index = Action {
		Ok(views.html.index("this will be a rest endpoint"))
  }
}