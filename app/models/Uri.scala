package models

import java.net.{URI, URISyntaxException}
import java.util.Date
import scala.util.Try
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.Logger
import org.postgresql.util.PSQLException
import controllers._

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
  
  def isBlacklisted: Boolean = DB.withConnection { implicit conn =>
    return BlacklistEvent.findBlacklistedByUri(id).nonEmpty
  }
  
  def isBlacklistedBy(source: Source): Boolean = DB.withConnection { implicit conn =>
    return BlacklistEvent.findBlacklistedByUri(id, Some(source)).nonEmpty
  }  
  
  def blacklist(source: Source, time: Long, endTime: Option[Long]=None): Boolean = DB.withConnection { implicit conn =>
    return BlacklistEvent.createOrUpdate(ReportedEvent(id, source, time, endTime))
  }
  
  def removeFromBlacklist(source: Source, time: Long): Boolean = DB.withConnection { implicit conn =>
    return BlacklistEvent.unBlacklist(id, source, time)
  }
  
  def requestReview(email: String, ip: Option[Long]=None, notes: Option[String]=None): Boolean = {
    return ReviewRequest.create(id, email, ip, notes)
  }
  
}

object Uri {
  
  private val BatchSize = Try(sys.env("SQLBATCH_SIZE").toInt).getOrElse(5000)
  
  def create(uriStr: String): Boolean = {
    return try {
      create(new ReportedUri(uriStr))
    } catch {
      case e: URISyntaxException => Logger.warn("Invalid Uri: '"+uriStr+"'\t"+e.getMessage)
      false
    }
  }
  
  def create(reported: ReportedUri): Boolean = DB.withTransaction { implicit conn =>
    val inserted = try {
      val uri = reported.uri
      SQL("""INSERT INTO uris (uri, reversed_host, hierarchical_part, path, sha2_256) 
    		SELECT {uri}, {reversedHost}, {hierarchicalPart}, {path}, {sha256} 
    		WHERE NOT EXISTS (SELECT 1 FROM uris WHERE sha2_256={sha256})""").on(
  		    "uri"->uri.toString,
  		    "reversedHost"->Host.reverse(uri),
  		    "hierarchicalPart"->reported.hierarchicalPart,
  		    "path"->uri.getRawPath,
  		    "sha256"->reported.sha256).executeUpdate()
  	} catch {
  	  case e: PSQLException => if (PostgreSql.isNotDupeError(e.getMessage)) {
  	    Logger.error(e.getMessage)
  	  }
  	  0
  	}
		return inserted > 0
  }
  
  def create(reported: List[String]): Int = DB.withTransaction { implicit conn =>
    return try {
      val sql = """INSERT INTO uris (uri, reversed_host, hierarchical_part, path, sha2_256) 
    		SELECT ?, ?, ?, ?, ? WHERE NOT EXISTS (SELECT 1 FROM uris WHERE sha2_256=?)"""    
      val ps = conn.prepareStatement(sql)
      reported.grouped(BatchSize).foldLeft(0) { (total, group) =>
	  		group.foreach { rep =>
	  		  Try(new ReportedUri(rep)).foreach { repUri =>
		  			ps.setString(1, repUri.uri.toString)
		  			ps.setString(2, Host.reverse(repUri.uri))
		  			ps.setString(3, repUri.hierarchicalPart)
		  			ps.setString(4, repUri.uri.getRawPath)
		  			ps.setString(5, repUri.sha256)
		  			ps.setString(6, repUri.sha256)
		  			ps.addBatch()
	  		  }
	      }
	  		val batch = ps.executeBatch()
	  		ps.clearBatch()
	  		total + batch.foldLeft(0)((cnt, b) => cnt + b)
      }
  	} catch {
  	  case e: PSQLException => if (PostgreSql.isNotDupeError(e.getMessage)) {
  	    Logger.error(e.getMessage)
  	  }
  	  0
  	}
  }   
  
  def findOrCreate(uriStr: String): Option[Uri] = {
    return try {
      findOrCreate(new ReportedUri(uriStr))
    } catch {
      case e: URISyntaxException => Logger.warn("Invalid URI: '"+uriStr+"'\t"+e.getMessage)
      None
    }
  }  
  
  def findOrCreate(reported: ReportedUri): Option[Uri] = {
    val findAttempt = find(reported.sha256)
    return if (findAttempt.isDefined) {
      findAttempt
    } else {
    	create(reported)
    	find(reported.sha256)
    }
  }
  
   def findOrCreateIds(reported: List[String]): List[Int] = {
  	val writes = create(reported)
  	Logger.info("Wrote "+writes+" new URIs")
  	return reported.grouped(10000).foldLeft(List.empty[Int]) { (ids, group) =>
      ids ++ find(group.map(u => ReportedUri.sha256(u))).map(_.id)
    }
  } 
  
  def asReportedUris(uris: List[String]): List[ReportedUri] = {
    return uris.map { uri =>
	    try {
	      Some(new ReportedUri(uri))
	    } catch {
	      case e: URISyntaxException => Logger.warn("Invalid URI: '"+uri+"'\t"+e.getMessage)
	      None
	    }
  	}.flatten
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
  
  def find(sha256s: List[String]): List[Uri] = DB.withTransaction { implicit conn =>
    return sha256s.size match {
      case 0 => List()
      case 1 => List(find(sha256s.head)).flatten
      case _ => try {
        sha256s.grouped(BatchSize).foldLeft(List.empty[Uri]) { case (list, group) =>
		      val sql = "SELECT * FROM uris WHERE sha2_256 in (?" + (",?"*(group.size-1)) + ")"
		      val ps = conn.prepareStatement(sql)
		      for (i <- 1 to group.size) {
		        ps.setString(i, group(i-1))
		      }
		      val rs = ps.executeQuery
		      Iterator.continually((rs, rs.next())).takeWhile(_._2).map { case (row, hasNext) =>
				    Some(Uri(
					    row.getInt("id"),
					    row.getString("uri"),
					    row.getString("reversed_host"),
					    row.getString("hierarchical_part"),
					    Option(row.getString("path")).getOrElse(""),
					    row.getString("sha2_256"),
					    row.getTimestamp("created_at").getTime / 1000
			  		))	
		      }.flatten.toList ++ list
        }
	    } catch {
	      case e: PSQLException => Logger.error(e.getMessage)
	      List()
	    }
    }
  }   
  
  def findByHierarchicalPart(hierarchicalPart: String): List[Uri] = DB.withConnection { implicit conn =>
    return try {
      val rs = SQL("SELECT * FROM uris WHERE hierarchical_part={hierarchicalPart}").on("hierarchicalPart"->hierarchicalPart).apply()
      if (rs.nonEmpty) rs.map(mapFromRow).flatten.toList else List()
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      List()
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
		    row[Date]("created_at").getTime / 1000    
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
  
  val uri: URI = new URI(ReportedUri.valid(uriStr))
  
  /* Using methods instead of vals to keep memory footprint minimal when importing large (million+) blacklists [WTSN-42] */
  def hierarchicalPart: String = uri.getRawAuthority + uri.getRawPath
  def sha256: String = Hash.sha256(uri.toString).getOrElse("")
  
  override def toString: String = uri.toString
  override def hashCode: Int = uri.hashCode
  override def equals(any: Any): Boolean = {
    return if (any.isInstanceOf[ReportedUri]) {
      uri.compareTo(any.asInstanceOf[ReportedUri].uri) == 0
    } else {
      false
    }
  }

}

object ReportedUri {
  private val schemeCheck = "^[a-zA-Z]+[a-zA-Z0-9+.\\-]+://.*"
  private def valid(url: String): String = if (url.matches(ReportedUri.schemeCheck)) url.trim else "http://" + url.trim
  def sha256(url: String): String = Hash.sha256(valid(url)).getOrElse("")
}