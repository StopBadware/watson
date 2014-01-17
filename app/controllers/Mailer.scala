package controllers

import scala.util.Try

object Mailer {
  
  private val username = sys.env("MANDRILL_USERNAME")
  private val apiKey = sys.env("MANDRILL_APIKEY")
  private val sendMail = Try(sys.env("SEND_EMAILS").equalsIgnoreCase("true")).getOrElse(false)
  
  def sendNoLongerBlacklisted(email: String, uri: String): Boolean = {
    //TODO WTSN-30 send no longer blacklisted notification
    return false	//DELME WTSN-30
  }
  
  def sendReviewClosedBad(email: String, uri: String, badCode: String): Boolean = {
    //TODO WTSN-30 send no longer blacklisted notification
    return false	//DELME WTSN-30
  }
  
  def sendReviewClosedCleanTts(email: String, uri: String): Boolean = {
    //TODO WTSN-30 send no longer blacklisted notification
    return false	//DELME WTSN-30
  }
  
  private def send(email: String, subject: String, body: String): Boolean = {
    //TODO WTSN-30 send email through mandrill
    return false	//DELME WTSN-30
  }

}