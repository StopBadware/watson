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
  
  def delete() = DB.withConnection { implicit conn =>
    try {
      SQL("DELETE FROM uris WHERE id={id}").on("id"->id).executeUpdate()
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
    }
  }
  
  def blacklist(source: String, time: Long) = DB.withConnection { implicit conn =>
    //TODO WTSN-11
  }
  
  def removeFromBlacklist(source: String, time: Long) = DB.withConnection { implicit conn =>
    //TODO WTSN-11
  }
  
}

object Uri {
  
  def create(reported: ReportedUri): Boolean = DB.withConnection { implicit conn =>
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
  	  case e: PSQLException => Logger.error(e.getMessage)
  	  0
  	}
		return inserted > 0
  }
  
  def findOrCreate(reported: ReportedUri): Option[Uri] = {
    create(reported)
    return find(reported.sha256)
  }
  
  def find(sha256: String): Option[Uri] = DB.withConnection { implicit conn =>
    return try {
      val rs = SQL("SELECT * FROM uris WHERE sha2_256={sha256}").on("sha256"->sha256).apply().headOption
      if (rs.isDefined) mapFromRow(rs.get) else None
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      None
    }
    
  }
  
  private def mapFromRow(row: SqlRow): Option[Uri] = {
    return try {  
	    Some(Uri(
		    row[Int]("id"),
		    row[String]("uri"),
		    row[String]("reversed_host"),
		    row[String]("hierarchical_part"),
		    row[Option[String]]("path").getOrElse(""),
		    row[String]("sha2_256"),
		    row[java.util.Date]("created_at").getTime / 1000    
  		))
    } catch {
      case e: Exception => None
    }
  }  
  
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