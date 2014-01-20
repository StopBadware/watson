package controllers

import java.io.OutputStreamWriter
import java.net.{HttpURLConnection, URL}
import scala.util.Try
import play.api._
import play.api.mvc._
import com.codahale.jerkson.Json._
import models.EmailTemplate

object Mailer extends Controller {
  
  private val username = sys.env("MANDRILL_USERNAME")
  private val apiKey = sys.env("MANDRILL_APIKEY")
  private val sendMail = Try(sys.env("SEND_EMAILS").equalsIgnoreCase("true")).getOrElse(false)
  private val smtpUrl = "https://mandrillapp.com/api/1.0/messages/send.json"
  private val noLongBlacklisted = "NoLongerBlacklisted"
  private val reviewClosedBad = "ReviewClosedBad"
  private val reviewClosedCleanTts = "ReviewClosedCleanTts"
    
  def sendNoLongerBlacklisted(email: String, uri: String): Boolean = {
    val template = EmailTemplate.find(noLongBlacklisted)
    val subjBody = if (template.isDefined) {
      (template.get.subject, template.get.body)
    } else {
      //TODO WTSN-30 insert default
      ("PLACEHOLDER SUBJECT", "PLACEHOLDER BODY")
    }
    //TODO WTSN-30 insert URI
    return send(sendReqJson(email, subjBody._1, subjBody._2, noLongBlacklisted))
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
    return try {
//      val url = new URL(smtpUrl)
      val url = new URL("http://127.0.0.1:1811/")	//DELME WTSN-30
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
      if (response != 200) {
        Logger.warn("Received HTTP status "+response+" sending email")
      }
      response == 200
    } catch {
      case e: Exception => Logger.error("Exception thrown sending email: "+e.getMessage)
      false
    }
  }
  
  private def sendReqJson(email: String, subject: String, htmlBody: String, tag: String): String = {
    return generate(Map(
      "key" -> apiKey, 
      "message" -> Map(
	      "from_email" -> "noreply@stopbadware.org",
	      "from_name" -> "StopBadware",
	      "to" -> Array(Map("email" -> email)),
	      "subject" -> subject,
	      "html" -> htmlBody,
	      "tags" -> Array(tag)
      )
    ))
  }
  
  
}