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
  private val noLongerBlacklisted = "NoLongerBlacklisted"
  private val reviewClosedBad = "ReviewClosedBad"
  private val reviewClosedCleanTts = "ReviewClosedCleanTts"
  private val reviewRequestReceived = "ReviewRequestReceived"
    
  def sendNoLongerBlacklisted(email: String, uri: String): Boolean = sendTemplate(noLongerBlacklisted, email, uri)
  
  def sendReviewClosedCleanTts(email: String, uri: String): Boolean = sendTemplate(reviewClosedCleanTts, email, uri)
  
  def sendReviewRequestReceived(email: String, uri: String): Boolean = sendTemplate(reviewRequestReceived, email, uri)
  
  def sendReviewClosedBad(email: String, uri: String, notes: String): Boolean = {
    sendTemplate(reviewClosedBad, email, uri, notes)
  }
  
  private def sendTemplate(templateName: String, email: String, uri: String, notes: String=""): Boolean = {
    val template = EmailTemplate.find(templateName)
    return if (template.isDefined) {
      val body = replacePlaceholders(template.get.body, uri, notes)
      val json = sendReqJson(email, template.get.subject, body, templateName)
      if (sendMail || email.endsWith("@stopbadware.org")) send(json) else json.nonEmpty
    } else {
      Logger.error("No email template found for "+templateName)
      false
    }
  }
  
  private def replacePlaceholders(content: String, uri: String, notes: String=""): String = {
    val link = "<a href='"+uri+"'>"+uri+"</a>"
    val safeLink = "<a href='"+"https://www.stopbadware.org/clearinghouse/search/?exactonly=true&url="+uri+"'>"+uri+"</a>"
    return content
  		.replaceAllLiterally("[URI]", link)
  		.replaceAllLiterally("[SAFE_URI]", safeLink)
  		.replaceAllLiterally("[TESTER_NOTES]", notes)
  }
  
  private def send(json: String): Boolean = {
    return try {
      val url = new URL(smtpUrl)
      val conn = url.openConnection.asInstanceOf[HttpURLConnection]
      conn.setRequestMethod("POST")
      conn.setRequestProperty("Content-Type", "application/json")
      conn.setDoOutput(true)
      val out = new OutputStreamWriter(conn.getOutputStream)
      out.write(json)
      out.close()
      val response = conn.getResponseCode
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
	      "tags" -> Array(tag),
	      "track_clicks" -> false,
	      "track_opens" -> false
      )
    ))
  }
  
  
}