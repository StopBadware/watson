package models

import java.net.{URI, URISyntaxException}
import anorm._
import play.api.db._
import play.api.Play.current
import org.postgresql.util.PSQLException
import controllers.{DbHandler => dbh, Hash}

case class Uri(
    id: Int,
    uri: String,
    reversedHost: String,
    hierarchicalPart: String,
    path: String,
    sha256: String,
    createdAt: Long
    ) {
  
}

object Uri {
  
  def create(reported: ReportedUri): Boolean = DB.withConnection { implicit c =>
    val foo = try { 
      val bar = SQL("INSERT INTO uris (uri, reversed_host, hierarchical_part, path, sha2_256) " +
    		"SELECT {uri}, {reversedHost}, {hierarchicalPart}, {path}, {sha256} " +
    		"WHERE NOT EXISTS (SELECT 1 FROM uris WHERE sha2_256={sha256})").on(
    		    "uri"->reported.uri.toString,
    		    "reversedHost"->reported.reversedHost,
    		    "hierarchicalPart"->reported.hierarchicalPart,
    		    "path"->reported.path,
    		    "sha256"->reported.sha256
    		)
    		println(bar)
    		bar.executeUpdate()
  	} catch {
  	  case e: PSQLException => println(e.getMessage())
  	}
    println(foo)	//DELME WTSN-11
		true //DELME WTSN-11 return Uri
  }
  
  def delete(id: Long) = DB.withConnection { implicit c =>
    //TODO WTSN-11
  }
  
  def blacklist = {}	//TODO WTSN-11
  def removeFromBlacklist = {} 	//TODO WTSN-11
  
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

}