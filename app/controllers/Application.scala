package controllers

import play.api._
import play.api.mvc._

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
  
}