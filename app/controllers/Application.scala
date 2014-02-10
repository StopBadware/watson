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
  
   def login = Action(parse.json) { implicit request =>
    val email = request.body.\("email").asOpt[String]
    val pw = request.body.\("pw").asOpt[String]
    if (email.isDefined && pw.isDefined) {
      val user = AuthAuth.authenticate(email.get, pw.get)
      if (user.isDefined) Ok(Json.obj("success" -> true)) else Unauthorized
    } else {
      BadRequest
    }
  }
  
  def register = Action {
    Ok(views.html.register())
  }
  
  def createAccount = Action(parse.json) { implicit request =>
    val email = request.body.\("email").asOpt[String]
    val pw = request.body.\("pw").asOpt[String]
    if (email.isDefined && pw.isDefined) {
      val stormpathAccount = AuthAuth.create(email.get, pw.get)
      val uname = email.get.split("@").headOption
      val userModel = if (stormpathAccount && uname.isDefined) User.create(uname.get, email.get) else false
      val created = stormpathAccount && userModel
      if (created) {
        Logger.info("Account created for '"+email.get+"'")
      }
  		Ok(Json.obj("created" -> created))
    } else {
      BadRequest
    }
  }
  
  def untrail(path: String) = Action {
    MovedPermanently("/" + path)
  }
  
  def javascriptRoutes = Action { implicit request =>
    Ok(Routes.javascriptRouter("jsRoutes")(
      routes.javascript.Application.createAccount,
      routes.javascript.Application.login
		)).as("text/javascript")
  }
  
}
