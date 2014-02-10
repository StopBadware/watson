package controllers

import play.api._
import play.api.mvc._
import play.api.libs.json._
import routes.javascript._
import models.User

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
    val email = request.body.\("email").asOpt[String]
    val pw = request.body.\("pw").asOpt[String]
    if (email.isDefined && pw.isDefined) {
      val stormpath = AuthAuth.create(email.get, pw.get)
      val uname = email.get.split("@").headOption
      val users = if (stormpath && uname.isDefined) User.create(uname.get, email.get) else false
  		Ok(Json.obj("created" -> (stormpath && users)))
    } else {
      BadRequest
    }
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