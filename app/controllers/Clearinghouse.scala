package controllers

import java.sql.Timestamp
import anorm._
import scala.util.Try
import play.api.db._
import play.api.libs.json._
import play.api.Logger
import play.api.mvc.Controller
import play.api.Play.current
import org.postgresql.util.PSQLException
import models.HostIpMapping

object Clearinghouse extends Controller with ApiSecured {
  
  private val blacklistLimit = Try(sys.env("BLACKLIST_LIMIT").toInt).getOrElse(250)
  
  def search = withAuth { implicit request =>
	  val host = request.getQueryString("host")
	  val ip = Try(request.getQueryString("ip").get.toLong).toOption
	  val asn = request.getQueryString("asn")
	  
	  if (host.isDefined) {
	    val json = {
	      val blacklistedOnly = request.getQueryString("blacklisted").getOrElse("").equalsIgnoreCase("true")
	      findUrisWithSiblingsAndChildren(host.get, blacklistedOnly)
	    }.splitAt(blacklistLimit)._1.map { chUri =>
	      Json.obj("uri_id" -> chUri.uriId, "uri" -> chUri.uri, "blacklisted" -> chUri.blacklisted)
	    }
	  	Ok(Json.obj("uris" -> json))
	  } else if (ip.isDefined) {
	    val chIp = ipSearch(ip.get)
	    val uris = uriSearch(ip.get.toString, false)
	    val json = {
	      Json.obj(
          "ip" -> ip.get,
          "num_blacklisted" -> chIp.numBlacklistedUris, 
          "asn" -> chIp.asNum, 
          "as_name" -> chIp.asName, 
          "as_country" -> chIp.asCounry,
      		"uris" -> uris.map { chUri =>
      		  Json.obj("uri_id" -> chUri.uriId, "uri" -> chUri.uri, "blacklisted" -> chUri.blacklisted)
      		}
	      )
	    }
	    Ok(json)
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
  
  def ipSearch(ip: Long): ChIp = DB.withConnection { implicit conn =>
    return try {
      val numUris = SQL("""SELECT COUNT(DISTINCT uris.id) AS count FROM host_ip_mappings JOIN uris ON 
        host_ip_mappings.reversed_host=uris.reversed_host JOIN blacklist_events ON uris.id=blacklist_events.uri_id WHERE 
        ip={ip} AND blacklisted=true AND last_resolved_at>={lastResolved}""")
        .on("ip" -> ip, "lastResolved" -> new Timestamp(HostIpMapping.lastResolvedAt * 1000))().head[Int]("count").toInt
      val row = SQL("""SELECT ip, number, name, country FROM ip_asn_mappings LEFT JOIN autonomous_systems ON asn=number 
        WHERE ip={ip} ORDER BY last_mapped_at DESC LIMIT 1""").on("ip" -> ip)().head
      ChIp(row[Long]("ip"), row[Option[Int]]("number"), row[Option[String]]("name"), row[Option[String]]("country"), numUris)
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      ChIp(ip)
    }
  }
  
  case class ChUri(uriId: Int, uri: String, blacklisted: Boolean)
  
  case class ChIp(
      ip: Long, 
      asNum: Option[Int]=None, 
      asName: Option[String]=None, 
      asCounry: Option[String]=None, 
      numBlacklistedUris: Int=0
  )

}
