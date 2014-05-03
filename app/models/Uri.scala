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
import models.enums.Source

case class Uri(
    id: Int,
    uri: String,
    reversedHost: String,
    hierarchicalPart: String,
    path: String,
    sha256: String,
    createdAt: Long
    ) {
  
  def delete(): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("DELETE FROM uris WHERE id={id}").on("id"->id).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
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
  
  private val schemeCheck = "^[a-zA-Z]+[a-zA-Z0-9+.\\-]+://.*"
    
  def ensureScheme(url: String): String = {
    val withScheme = if (url.matches(schemeCheck)) url.trim else "http://" + url.trim
    if (withScheme.substring(withScheme.indexOf("//")+2).contains("/")) withScheme else withScheme+"/"
  }
  
  private def sha256(url: String): String = Hash.sha256(ensureScheme(url)).getOrElse("")
  
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
  
  def create(uris: List[String]): Int = DB.withTransaction { implicit conn =>
    return try {
      val sql = """INSERT INTO uris (uri, reversed_host, hierarchical_part, path, sha2_256) 
    		SELECT ?, ?, ?, ?, ? WHERE NOT EXISTS (SELECT 1 FROM uris WHERE sha2_256=?)"""    
      val ps = conn.prepareStatement(sql)
      uris.grouped(PostgreSql.batchSize).foldLeft(0) { (total, group) =>
	  		group.foreach { uri =>
	  		  Try(new ReportedUri(uri)).foreach { repUri =>
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
  
  def findOrCreate(uri: String): Option[Uri] = {
    return try {
      findOrCreate(new ReportedUri(uri))
    } catch {
      case e: URISyntaxException => Logger.warn("Invalid URI: '"+uri+"'\t"+e.getMessage)
      None
    }
  }  
  
  def findOrCreate(reported: ReportedUri): Option[Uri] = {
    val findAttempt = findBySha256(reported.sha256)
    return if (findAttempt.isDefined) {
      findAttempt
    } else {
    	create(reported)
    	findBySha256(reported.sha256)
    }
  }
  
  def findOrCreateIds(uris: List[String]): List[Int] = {
  	val writes = create(uris)
  	Logger.info("Wrote " + writes + " new URIs")
  	return findIds(uris)
  }
  
  def findIds(uris: List[String]): List[Int] = {
  	return uris.grouped(10000).foldLeft(List.empty[Int]) { (ids, group) =>
      ids ++ findBySha256(group.map(u => sha256(u))).map(_.id)
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
  
  def find(id: Int): Option[Uri] = DB.withConnection { implicit conn =>
    return Try(mapFromRow(SQL("SELECT * FROM uris WHERE id={id} LIMIT 1").on("id"->id)().head)).getOrElse(None)
  }
  
  def find(ids: List[Int]): List[Uri] = DB.withConnection { implicit conn =>
    return ids.size match {
      case 0 => List()
      case _ => try {
        ids.grouped(PostgreSql.batchSize).foldLeft(List.empty[Uri]) { case (list, group) =>
		      val sql = "SELECT * FROM uris WHERE id in (?" + (",?"*(group.size-1)) + ")"
		      val ps = conn.prepareStatement(sql)
		      for (i <- 1 to group.size) {
		        ps.setInt(i, group(i-1))
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
  
  def findBySha256(sha256: String): Option[Uri] = DB.withConnection { implicit conn =>
    return Try(mapFromRow(SQL("SELECT * FROM uris WHERE sha2_256={sha256} LIMIT 1")
      .on("sha256"->sha256)().head)).getOrElse(None)
  }
  
  def findBySha256(sha256s: List[String]): List[Uri] = DB.withTransaction { implicit conn =>
    return sha256s.size match {
      case 0 => List()
      case 1 => List(findBySha256(sha256s.head)).flatten
      case _ => try {
        sha256s.grouped(PostgreSql.batchSize).foldLeft(List.empty[Uri]) { case (list, group) =>
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
    return Try {  
	    Uri(
		    row[Int]("id"),
		    row[String]("uri"),
		    row[String]("reversed_host"),
		    row[String]("hierarchical_part"),
		    row[Option[String]]("path").getOrElse(""),
		    row[String]("sha2_256"),
		    row[Date]("created_at").getTime / 1000    
  		)
    }.toOption
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
  
  val uri: URI = new URI(Uri.ensureScheme(uriStr))
  
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
