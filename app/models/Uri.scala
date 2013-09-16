package models

import java.net.{URI, URISyntaxException}
import controllers.{DbHandler => dbh, Hash}

@throws[URISyntaxException]
case class Uri(uriStr: String) {
  
  val uri: URI = {
    val schemeCheck = "^[a-zA-Z]+[a-zA-Z0-9+.\\-]+://.*"
    val withScheme = if (uriStr.matches(schemeCheck)) uriStr else "http://" + uriStr
    new URI(withScheme)
  }
  val path = uri.getRawPath
  val query = uri.getRawQuery
  val hierarchicalPart = uri.getRawAuthority + uri.getRawPath
  lazy val reversedHost = Host.reverse(uri.getHost)
  lazy val sha2 = Hash.sha2(uriStr)
  
  def blacklist(source: String, time: Long) = dbh.blacklist(this, source, time)
  def removeFromBlacklist(source: String, time: Long) = dbh.removeFromBlacklist(this, source, time)

}