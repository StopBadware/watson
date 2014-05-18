package controllers

import anorm._
import scala.util.Try
import play.api.db._
import play.api.libs.json._
import play.api.Logger
import play.api.mvc.Controller
import play.api.Play.current
import org.postgresql.util.PSQLException

object Clearinghouse extends Controller with ApiSecured {
  
  private val blacklistLimit = Try(sys.env("BLACKLIST_LIMIT").toInt).getOrElse(250)
  
  def search = withAuth { implicit request =>
	  val host = request.getQueryString("host")
	  val ip = request.getQueryString("ip")
	  val asn = request.getQueryString("asn")
	  
	  if (host.isDefined) {
	    val json = {
	      val blacklistedOnly = request.getQueryString("blacklisted").getOrElse("").equalsIgnoreCase("true")
	      findUrisWithSiblingsAndChildren(host.get, blacklistedOnly)
	    }.splitAt(blacklistLimit)._1.map { chUri =>
	      Json.obj("uri_id" -> chUri.uriId, "uri" -> chUri.uri, "blacklisted" -> chUri.blacklisted)
	    }
	  	Ok(Json.obj("results" -> json))
	  } else if (ip.isDefined) {
	    NotFound	//TODO WTSN-50
	  } else if (asn.isDefined) {
	    NotFound	//TODO WTSN-50
	  } else {
	    BadRequest
	  }
	}
  
  def findUrisWithSiblingsAndChildren(host: String, blacklistedOnly: Boolean): List[ChUri] = {
    val initial = uriSearch(host, blacklistedOnly)
    if (initial.size >= blacklistLimit) {
      return initial
    }

    val labels = host.split("\\.").toList
    val children = uriSearch("%."+host, blacklistedOnly)
    if (initial.size + children.size >= blacklistLimit || labels.size < 3) {
      return initial ++ children
    }
    
    val parent = uriSearch(labels.tail.mkString("."), blacklistedOnly)
    if (initial.size + children.size + parent.size >= blacklistLimit) {
      return initial ++ children ++ parent
    }
    
    val siblings = uriSearch(("%" +: labels.tail).mkString("."), blacklistedOnly)
    return initial ++ children ++ parent ++ siblings
  } 
  
  private def uriSearch(host: String, blacklistedOnly: Boolean): List[ChUri] = DB.withConnection { implicit conn =>
    val reversedHost = Host.reverse(host)
    return try {
      val sql = "SELECT uris.id, uris.uri, blacklisted, MAX(blacklisted_at) AS most_recent FROM uris JOIN blacklist_events "+ 
        "ON blacklist_events.uri_id=uris.id WHERE reversed_host LIKE {reversedHost} "+
        (if (blacklistedOnly) "AND blacklisted=true " else "") +
        "GROUP BY uris.id, blacklisted ORDER BY blacklisted DESC, most_recent DESC LIMIT {limit}"
      SQL(sql).on("reversedHost" -> reversedHost, "limit" -> blacklistLimit)().map { row => 
        ChUri(row[Int]("id"), row[String]("uri"), row[Boolean]("blacklisted"))
      }.toList
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      List()
    }
  }
  
  case class ChUri(uriId: Int, uri: String, blacklisted: Boolean)

}
