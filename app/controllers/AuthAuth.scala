package controllers

import play.api._
import play.api.mvc._
import models.User

object AuthAuth extends Controller {
  
  val apiKey = sys.env("STORMPATH_API_KEY_ID")
  val apiSecret = sys.env("STORMPATH_API_KEY_SECRET")
  val apiUrl = sys.env("STORMPATH_URL")
  
  def create(email: String, password: String): Boolean = {
    return false	//TODO WTSN-48
  }
  
  def authenticate(unameOrEmail: String, password: String): Option[User] = {
    return None	//TODO WTSN-48
  }
  
  private class StormpathUser() {
    
    def enable(): Boolean = {
      return false	//TODO WTSN-48
    }
    
    def disable(): Boolean = {
      return false //TODO WTSN-48
    }
    
  }

}