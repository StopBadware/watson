package controllers

import play.api._
import play.api.mvc._
import scala.util.Try
import models.User
import com.stormpath.sdk.account._
import com.stormpath.sdk.application.{Application => StormpathApp}
import com.stormpath.sdk.client._

object AuthAuth extends Controller {
  
  private val apiKey = sys.env("STORMPATH_API_KEY_ID")
	private val apiSecret = sys.env("STORMPATH_API_KEY_SECRET")
  private val apiUrl = sys.env("STORMPATH_URL")
  private val client =  new ClientBuilder().setApiKey(apiKey, apiSecret).build
  private val app = client.getResource(apiUrl, classOf[StormpathApp])
  
  def create(email: String, password: String): Boolean = {
    return if (email.endsWith("@stopbadware.org")) {
	    val username = email.split("@").head.toLowerCase
	    val account = client.instantiate(classOf[Account])
	    account.setEmail(email)
	    account.setGivenName(username.toUpperCase)
	    account.setPassword(password)
	    account.setSurname("STOPBADWARE")
	    account.setUsername(username)
	    Try(app.createAccount(account)).isSuccess
    } else {
      false
    }
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