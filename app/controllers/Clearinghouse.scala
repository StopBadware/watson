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
import models._
import models.cr._

object Clearinghouse extends Controller with ApiSecured {
  
  private val blacklistLimit = Try(sys.env("BLACKLIST_LIMIT").toInt).getOrElse(250)
  
  def findUri(id: Int) = withAuth { implicit request =>
    val uri = Uri.find(id)
    if (uri.isDefined) {
      val u = uri.get
      val ipMapping = Try {
      	val hostIp = HostIpMapping.findByHost(u.reversedHost).maxBy(_.lastresolvedAt)
      	(Some(hostIp.ip), Some(hostIp.lastresolvedAt))
      }.getOrElse((None, None))
      val asInfo = Try(AutonomousSystem.find(IpAsnMapping.findByIp(ipMapping._1.get).maxBy(_.lastMappedAt).asn).get).toOption
      val json = Json.obj(
	      "ip" -> ipMapping._1,
	      "ip_resolved_at" -> ipMapping._2, 
	      "asn" -> Try(asInfo.get.number).toOption, 
	      "as_name" -> Try(asInfo.get.name).toOption, 
	      "as_country" -> Try(asInfo.get.country).toOption,
	  		"blacklist_events" -> blacklistEventJson(u.id),
	  		"reviews" -> reviewsJson(u.id),
	  		"community_reports" -> communityReportsJson(u.id)
	    )
    	Ok(json)
    } else {
      NotFound
    }
  }
  
  private def blacklistEventJson(uriId: Int): List[JsObject] = {
    BlacklistEvent.findByUri(uriId).map { event =>
		  Json.obj(
	      "source" -> event.source.abbr,
	      "status" -> event.blacklisted,
	      "blacklisted_at" -> event.blacklistedAt,
	      "unblacklisted_at" -> event.unblacklistedAt
      )
		}
  }
  
  private def reviewsJson(uriId: Int): List[JsObject] = {
    Review.findByUri(uriId).map { review =>
		  Json.obj(
	      "status" -> review.status.toString,
	      "bad_code" -> Try(ReviewCode.findByReview(review.id).maxBy(_.updatedAt).badCode).toOption,
	      "review_opened" -> review.createdAt,
	      "review_closed" -> review.statusUpdatedAt
      )
		}
  }
  
  private def communityReportsJson(uriId: Int): List[JsObject] = {
    CommunityReport.findByUri(uriId).map { cr =>
		  Json.obj(
	      "description" -> cr.description,
	      "bad_code" -> cr.badCode,
	      "type" -> Try(CrType.find(cr.crTypeId.get).get.crType).toOption,
	      "reported_at" -> cr.reportedAt
      )
		}
  }
  
  def search = withAuth { implicit request =>
	  val host = request.getQueryString("host")
	  val ip = Try(request.getQueryString("ip").get.toLong).toOption
	  val asn = Try(request.getQueryString("asn").get.toInt).toOption
	  
	  if (host.isDefined) {
	    val blacklistedOnly = request.getQueryString("blacklisted").getOrElse("").equalsIgnoreCase("true")
	  	Ok(uriJson(host.get, blacklistedOnly))
	  } else if (ip.isDefined) {
	    Ok(ipJson(ip.get))
	  } else if (asn.isDefined) {
	    Ok(asnJson(asn.get))
	  } else {
	    BadRequest
	  }
	}
  
  private def uriJson(host: String, blacklistedOnly: Boolean): JsObject = {
    val uris = findUrisWithSiblingsAndChildren(host, blacklistedOnly).splitAt(blacklistLimit)._1.map { chUri =>
      Json.obj("uri_id" -> chUri.uriId, "uri" -> chUri.uri, "blacklisted" -> chUri.blacklisted)
    }
    return Json.obj("uris" -> uris)
  }
  
  private def ipJson(ip: Long): JsObject = {
    val chIp = ipSearch(ip)
    val uris = uriSearch(ip.toString, false)
    return Json.obj(
      "ip" -> ip,
      "num_blacklisted_uris" -> chIp.numBlacklistedUris, 
      "asn" -> chIp.asNum, 
      "as_name" -> chIp.asName, 
      "as_country" -> chIp.asCountry,
  		"uris" -> uris.map { chUri =>
  		  Json.obj("uri_id" -> chUri.uriId, "uri" -> chUri.uri, "blacklisted" -> chUri.blacklisted)
  		}
    )
  }
  
  private def asnJson(asn: Int): JsObject = {
    val as = AutonomousSystem.find(asn)
    val ips = blacklistedIps(asn)
    Json.obj(
      "num_blacklisted_ips" -> ips.size,
      "num_blacklisted_uris" -> blacklistedUrisCount(ips),
      "asn" -> asn, 
      "as_name" -> Try(as.get.name).toOption, 
      "as_country" -> Try(as.get.country).toOption
    )
  }
  
  def findUrisWithSiblingsAndChildren(host: String, blacklistedOnly: Boolean=false): List[ChUri] = {
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
      val numUris = blacklistedUrisCount(List(ip))
      val row = SQL("""SELECT ip, number, name, country FROM ip_asn_mappings LEFT JOIN autonomous_systems ON asn=number 
        WHERE ip={ip} ORDER BY last_mapped_at DESC LIMIT 1""").on("ip" -> ip)().head
      ChIp(row[Long]("ip"), row[Option[Int]]("number"), row[Option[String]]("name"), row[Option[String]]("country"), numUris)
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      ChIp(ip)
    }
  }
  
  def blacklistedUrisCount(ips: List[Long]): Int = DB.withTransaction { implicit conn =>
    return try {
      val resolvedAt = new Timestamp(HostIpMapping.lastResolvedAt * 1000)
      val sql = """SELECT COUNT(DISTINCT uris.id) AS count FROM host_ip_mappings JOIN uris ON 
        host_ip_mappings.reversed_host=uris.reversed_host JOIN blacklist_events ON uris.id=blacklist_events.uri_id 
        WHERE blacklisted=true AND last_resolved_at=? AND ip IN (?"""+(",?" * (ips.size - 1))+")"
      val ps = conn.prepareStatement(sql)
      ps.setTimestamp(1, resolvedAt)
      ips.foldLeft(2) { (i, ip) =>
        ps.setLong(i, ip)
        i + 1
      }
    	val rs = ps.executeQuery
    	Iterator.continually((rs, rs.next())).takeWhile(_._2).foldLeft(0) { (cnt, tuple) =>
    	  cnt + tuple._1.getLong("count").toInt
    	}
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      0
    }
  }
  
  def blacklistedIps(asn: Int): List[Long] = DB.withTransaction { implicit conn =>
    return try {
      SQL("""SELECT DISTINCT ip_asn_mappings.ip AS ip FROM ip_asn_mappings JOIN host_ip_mappings ON 
        ip_asn_mappings.ip=host_ip_mappings.ip JOIN uris ON host_ip_mappings.reversed_host=uris.reversed_host JOIN blacklist_events 
        ON uris.id=blacklist_events.uri_id WHERE asn={asn} AND blacklisted=true AND last_mapped_at={resolvedAt}""")
        .on("asn" -> asn, "resolvedAt" -> new Timestamp(HostIpMapping.lastResolvedAt * 1000))().map(_[Long]("ip")).toList
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      List()
    }
  }
  
  case class ChUri(uriId: Int, uri: String, blacklisted: Boolean)
  
  case class ChIp(
      ip: Long, 
      asNum: Option[Int]=None, 
      asName: Option[String]=None, 
      asCountry: Option[String]=None, 
      numBlacklistedUris: Int=0
  )
  
}
