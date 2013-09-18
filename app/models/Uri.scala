package models

import java.net.{URI, URISyntaxException}
import controllers.{DbHandler => dbh, Hash}
import com.mongodb.casbah.Imports._
import com.mongodb.BasicDBObject

/**
 * Represents a MongoDB document from the uris collection
 */
protected class Uri(uriDoc: DBObject) {
  
  val sha256 = uriDoc.get("sha256")
  
}

object Uri {
  
  def apply(uriDoc: DBObject): Uri = new Uri(uriDoc)
  
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