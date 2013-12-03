package models

import java.net.{URI, URISyntaxException}
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.Logger
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
  
  def mapFromRow(row: SqlRow): Uri = {
    return Uri(
	    row[Int]("id"),
	    row[String]("uri"),
	    row[String]("reversed_host"),
	    row[String]("hierarchical_part"),
	    row[String]("path"),
	    row[String]("sha2_256"),
	    row[Long]("createdAt")    
  		)
  }
  
  def create(reported: ReportedUri): Boolean = DB.withConnection { implicit c =>
    val inserted = try { 
      SQL("""INSERT INTO uris (uri, reversed_host, hierarchical_part, path, sha2_256) 
    		SELECT {uri}, {reversedHost}, {hierarchicalPart}, {path}, {sha256} 
    		WHERE NOT EXISTS (SELECT 1 FROM uris WHERE sha2_256={sha256})""").on(
    		    "uri"->reported.uri.toString,
    		    "reversedHost"->reported.reversedHost,
    		    "hierarchicalPart"->reported.hierarchicalPart,
    		    "path"->reported.path,
    		    "sha256"->reported.sha256
    		).executeUpdate()
  	} catch {
  	  case e: PSQLException => Logger.error(e.getMessage())
  	  0
  	}
		return inserted > 0
  }
  
  def delete(id: Long) = DB.withConnection { implicit c =>
    //TODO WTSN-11
  }
  
  def find(sha256: String): Option[Uri] = DB.withConnection { implicit c =>
    try {
      val rs = SQL("SELECT * FROM uris WHERE sha2_256={sha256}").on("sha256"->sha256).apply().headOption
      if (rs.isDefined) Some(mapFromRow(rs.get)) else None
    } catch {
      case e: PSQLException => Logger.error(e.getMessage())
      None
    }
    
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
  lazy val sha256 = Hash.sha256(uri.toString).getOrElse("")
  
  override def hashCode: Int = uri.hashCode
  override def toString: String = uri.toString 

}