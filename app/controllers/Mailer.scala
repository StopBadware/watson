package controllers

import java.io.OutputStreamWriter
import java.net.{HttpURLConnection, URL}
import scala.util.Try
import play.api._
import play.api.mvc._
import com.codahale.jerkson.Json._

object Mailer extends Controller {
  
  private val username = sys.env("MANDRILL_USERNAME")
  private val apiKey = sys.env("MANDRILL_APIKEY")
  private val sendMail = Try(sys.env("SEND_EMAILS").equalsIgnoreCase("true")).getOrElse(false)
  private val smtpUrl = "https://mandrillapp.com/api/1.0/messages/send.json"
  
  def sendNoLongerBlacklisted(email: String, uri: String): Boolean = {
    //TODO WTSN-30 send no longer blacklisted notification
    val subject = "TEST"
    val body = "EOM"
    return send(sendReqJson(email, subject, body, "NoLongerBlacklisted"))
  }
  
  def sendReviewClosedBad(email: String, uri: String, badCode: String): Boolean = {
    //TODO WTSN-30 send no longer blacklisted notification
    return false	//DELME WTSN-30
  }
  
  def sendReviewClosedCleanTts(email: String, uri: String): Boolean = {
    //TODO WTSN-30 send no longer blacklisted notification
    return false	//DELME WTSN-30
  }
  
  private def send(json: String): Boolean = {
    try {
      val url = new URL(smtpUrl)
//      val url = new URL("http://127.0.0.1:1811/")	//DELME WTSN-30
      val conn = url.openConnection.asInstanceOf[HttpURLConnection]
      conn.setRequestMethod("POST")
      conn.setRequestProperty("Content-Type", "application/json")
      conn.setDoOutput(true)
      val out = new OutputStreamWriter(conn.getOutputStream)
      out.write(json)
      out.close()
      val response = conn.getResponseCode
      println(response)	//DELME WTSN-30
      conn.disconnect()
    } catch {
      case e: Exception => Logger.error("Exception thrown sending email: "+e.getMessage)
    }
    return false	//DELME WTSN-30
  }
  
  private def sendReqJson(email: String, subject: String, htmlBody: String, tag: String): String = {
    return generate(Map(
      "key" -> apiKey, 
      "message" -> Map(
	      "from_email" -> "noreply@stopbadware.org",  
	      "to" -> Array(Map("email" -> email)),
	      "subject" -> subject,
	      "html" -> htmlBody,
	      "tags" -> Array(tag)
      )
    ))
  }

}