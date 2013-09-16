package models

import java.net.URL
import controllers.{DbHandler => dbh, Hash}

case class Uri(uri: String) {
  
  private val url: URL = {
    val schemeCheck = "[a-zA-Z]+[a-zA-Z+.\\-]+://"
    val withScheme = if (uri.matches(schemeCheck)) uri else "http://" + uri
    new URL(withScheme)
  }
  val path = url.getPath
  val query = url.getQuery
  val hierarchicalPart = url.getAuthority + url.getPath
  lazy val reversedHost = Host.reverse(url.getHost)
  lazy val sha2 = Hash.sha2(uri)
  
  def blacklist(source: String, time: Long) {
    //TODO WTSN-11 change blacklisted flag if not already blacklisted by any source
    //TODO WTSN-11 add source/time entry if not already blacklisted by this source
  }
  
  def removeFromBlacklist(source: String, time: Long) {
    //TODO WTSN-11 change blacklisted flag if not blacklisted by any other source
    //TODO WTSN-11 add cleantime for this source 
  }
}