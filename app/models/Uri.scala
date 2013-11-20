package models

import java.net.{URI, URISyntaxException}
import controllers.{DbHandler => dbh, Hash}

protected class Uri {
  
  val uri = Nil //TODO WTSN-11
  val path = Nil //TODO WTSN-11
  val query = Nil //TODO WTSN-11
  val hierPart = Nil //TODO WTSN-11
  val reversedHost = Nil //TODO WTSN-11
  val sha256 = Nil //TODO WTSN-11
  
}

object Uri {
  
  def apply(): Uri = new Uri()	//TODO WTSN-11
  
}

/**
 * Raw reported/blacklisted URI
 * @param uriStr - if the passed string does not have a scheme 'http' will be assumed
 * by prefixing the passed string with 'http://'
 * @throws URISyntaxException if a valid URI (as specified by RFC-2396) cannot be parsed
 */
@throws[URISyntaxException]
class ReportedUri(uriStr: String) {
  
  val uri: URI = {
    val schemeCheck = "^[a-zA-Z]+[a-zA-Z0-9+.\\-]+://.*"
    val withScheme = if (uriStr.matches(schemeCheck)) uriStr else "http://" + uriStr
    new URI(withScheme)
  }
  val path = uri.getRawPath
  val query = uri.getRawQuery
  val hierarchicalPart = uri.getRawAuthority + uri.getRawPath
  lazy val reversedHost = Host.reverse(uri.getHost)
  lazy val sha256 = Hash.sha256(uri.toString)
  
  override def hashCode: Int = uri.hashCode
  override def toString: String = uri.toString 
  def blacklist(source: String, time: Long) = dbh.blacklist(ReportedUri.this, source, time)
  def removeFromBlacklist(source: String, time: Long) = dbh.removeFromBlacklist(ReportedUri.this, source, time)

}