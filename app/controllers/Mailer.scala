package controllers

import java.io.OutputStreamWriter
import java.net.{HttpURLConnection, URL}
import java.text.SimpleDateFormat
import java.util.Date
import scala.util.Try
import scala.actors.Future
import scala.actors.Futures.future
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
    
  def sendNoLongerBlacklisted(email: String, uri: String): Future[Boolean] = sendTemplate(noLongerBlacklisted, email, uri)
  
  def sendReviewClosedCleanTts(email: String, uri: String): Future[Boolean] = sendTemplate(reviewClosedCleanTts, email, uri)
  
  def sendReviewRequestReceived(email: String, uri: String): Future[Boolean] = sendTemplate(reviewRequestReceived, email, uri)
  
  def sendReviewClosedBad(email: String, uri: String, badCode: String): Future[Boolean] = {
    sendTemplate(reviewClosedBad, email, uri, badCode)
  }
  
  def sendPublicKey(email: String, key: String): Future[Boolean] = {
    val body = "<h2>Watson API public key corresponding to the secret key shown at time of request:</h2><h2>"+key+"</h2>"
		return sendEmail("Watson API Public Key", body, email, tag="API Key")
  }
  
  def sendGoogleRescanRequest(uris: Set[String]): Future[Boolean] = {
    val to = sys.env("GOOG_RESCAN_TO")
    val cc = Try(sys.env("GOOG_RESCAN_CC")).toOption
    val sendTo = Array(
      Map("email" -> to, "type" -> "to") ++ 
      (if (cc.isDefined) Map("email" -> cc.get, "type" -> "cc") else Map())
    )
    val attachment = Array(
      Map(
        "type" -> "text/plain", 
        "name" -> ("Appeals" + (new SimpleDateFormat("MMddyyyy")).format(new Date()) + ".txt"),
        "content" -> Text.encodeBase64(uris.mkString("\n"))
      )
    )
    val json = generate(Map(
      "key" -> apiKey, 
      "message" -> Map(
	      "from_email" -> "reviews@stopbadware.org",
	      "from_name" -> "StopBadware Reviews",
	      "to" -> sendTo,
	      "subject" -> "StopBadware: New Appeals (assorted)",
	      "attachments" -> attachment,
	      "tags" -> Array("GoogleRescanRequest"),
	      "track_clicks" -> false,
	      "track_opens" -> false
      )
    ))
    return future(if (sendMail || to.endsWith("@stopbadware.org")) send(json) else uris.nonEmpty)
  }
  
  private def sendTemplate(templateName: String, email: String, uri: String, badCode: String=""): Future[Boolean] = {
    val template = EmailTemplate.find(templateName)
    return if (template.isDefined) {
      sendEmail(template.get.subject, template.get.body, email, uri, badCode, templateName)
    } else {
      Logger.error("No email template found for "+templateName)
      future(false)
    }
  }
  
  def sendEmail(subject: String, body: String, email: String, uri: String="", badCode: String="", tag: String=""): Future[Boolean] = {
    return future {
      val json = sendReqJson(email, subject, replacePlaceholders(body, uri, badCode), tag)
      if (sendMail || email.endsWith("@stopbadware.org")) send(json) else json.nonEmpty
    }
  }
  
  private def replacePlaceholders(content: String, uri: String, badCode: String=""): String = {
    val link = "<a href='"+uri+"'>"+uri+"</a>"
    val safeLink = "<a href='"+"https://www.stopbadware.org/clearinghouse/search/?exactonly=true&url="+uri+"'>"+uri+"</a>"
    return content
  		.replaceAllLiterally("[URI]", link)
  		.replaceAllLiterally("[SAFE_URI]", safeLink)
  		.replaceAllLiterally("[BAD_CODE]", xml.Utility.escape(badCode))
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