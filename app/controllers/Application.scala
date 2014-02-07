package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import routes.javascript._

object Application extends Controller with JsonMapper {
  
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
    val email = request.body.\("email").asOpt[String].getOrElse("")
    val pw = request.body.\("pw").asOpt[String].getOrElse("")
    val created = true		//TODO WTSN-52 create account
    val msg = ""					//TODO WTSN-52 create should return Try instead of Boolean
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