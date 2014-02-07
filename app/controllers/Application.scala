package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import routes.javascript._

object Application extends Controller {
  
  def index = Action {
    Ok(views.html.index("Watson"))
  }
  
  def welcome = Action {
    Ok(views.html.welcome())
  }
  
  def register = Action {
    Ok(views.html.register())
  }
  
  def createAccount = Action(parse.json) { implicit request =>
    println(request)			//DELME WTSN-52
    println(request.body)	//DELME WTSN-52
    val created = true	//TODO WTSN-52
    val msg = ""	//TODO WTSN-52
     Ok(Json.obj("created" -> created, "msg" -> msg))
  }
  
  def untrail(path: String) = Action {
    MovedPermanently("/" + path)
  }
  
  def javascriptRoutes = Action { implicit request =>
    Ok(Routes.javascriptRouter("jsRoutes")(
      routes.javascript.Application.createAccount
		)).as("text/javascript")
  }
  
}