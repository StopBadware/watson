package controllers

import scala.collection.JavaConversions._
import play.api._
import play.api.mvc._
import scala.util.Try
import models.User
import com.stormpath.sdk.account._
import com.stormpath.sdk.authc._
import com.stormpath.sdk.client._
import com.stormpath.sdk.application.{Application => StormpathApp}
import com.stormpath.sdk.resource.ResourceException

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
	    try {
	    	val created = app.createAccount(account)
	    	created.getStatus.equals(AccountStatus.UNVERIFIED)
	    } catch {
	      case e: ResourceException => Logger.warn("Unable to create account for '"+email+"': "+e.getMessage)
	      false
	    }
    } else {
      false
    }
  }
  
  def authenticate(unameOrEmail: String, password: String): Option[User] = {
    val request = new UsernamePasswordRequest(unameOrEmail, password)
    return try {
      User.findByUsername(app.authenticateAccount(request).getAccount.getUsername)
    } catch {
      case e: ResourceException => Logger.warn("Authentication failure for '"+unameOrEmail+"': "+e.getDeveloperMessage)
      None
    } finally {
      request.clear()
    }
  }
  
  def delete(email: String): Boolean = {
    find(email).foreach(_.delete())
    return find(email).size == 0
  }
  
  def sendResetMail(email: String): Boolean = Try(app.sendPasswordResetEmail(email)).isSuccess
  
  def enable(email: String): Boolean = setStatus(email, AccountStatus.ENABLED)
  
  def disable(email: String): Boolean = setStatus(email, AccountStatus.DISABLED)
  
  private def setStatus(email: String, status: AccountStatus): Boolean = {
    return find(email).map { account =>
      account.setStatus(status)
      account.save()
      account.getStatus
    }.forall(_.equals(status))
  }
  
  private def find(email: String): AccountList = app.getAccounts(Accounts.where(Accounts.email.eqIgnoreCase(email))) 
  
}