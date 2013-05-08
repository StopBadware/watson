package controllers

import play.api._
import play.api.mvc._

object Rest extends Controller {

	def index = Action {
		Ok(views.html.index("this will be a rest endpoint"))
  }
	
	def timeoflast(source: String) = Action {
		Ok(views.html.index("timeoflast for "+source))
  }
	
	def blacklist(source: String) = Action {
		Ok(views.html.index("blacklist"))
  }
	
	def cleanlist(source: String) = Action {
		Ok(views.html.index("cleanlist"))
  }
	
	def appeals(source: String) = Action {
		Ok(views.html.index("appeals"))
  }
}