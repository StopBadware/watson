package controllers

import play.api._
import play.api.mvc._
import models.User

object AuthAuth extends Controller {
  
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