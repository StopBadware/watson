package models

import java.net.{URI, URISyntaxException}
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
    return BlacklistEvent.markNoLongerBlacklisted(id, source, time)
  }
  
}

object Uri {
  
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
      SQL("""INSERT INTO uris (uri, reversed_host, hierarchical_part, path, sha2_256) 
    		SELECT {uri}, {reversedHost}, {hierarchicalPart}, {path}, {sha256} 
    		WHERE NOT EXISTS (SELECT 1 FROM uris WHERE sha2_256={sha256})""").on(
  		    "uri"->reported.uri.toString,
  		    "reversedHost"->reported.reversedHost,
  		    "hierarchicalPart"->reported.hierarchicalPart,
  		    "path"->reported.path,
  		    "sha256"->reported.sha256).executeUpdate()
  	} catch {
  	  case e: PSQLException => if (PostgreSql.isNotDupeError(e.getMessage)) {
  	    Logger.error(e.getMessage)
  	  }
  	  0
  	}
		return inserted > 0
  }
  
  def create(reported: List[ReportedUri]): Int = DB.withTransaction { implicit conn =>
    val inserted = try {
//      SQL("""INSERT INTO uris (uri, reversed_host, hierarchical_part, path, sha2_256) 
//    		SELECT {uri}, {reversedHost}, {hierarchicalPart}, {path}, {sha256} 
//    		WHERE NOT EXISTS (SELECT 1 FROM uris WHERE sha2_256={sha256})""").on(
//  		    "uri"->reported.uri.toString,
//  		    "reversedHost"->reported.reversedHost,
//  		    "hierarchicalPart"->reported.hierarchicalPart,
//  		    "path"->reported.path,
//  		    "sha256"->reported.sha256).executeUpdate()
      val sql = SqlQuery("""INSERT INTO uris (uri, reversed_host, hierarchical_part, path, sha2_256) 
    		SELECT {uri}, {reversedHost}, {hierarchicalPart}, {path}, {sha256} 
    		WHERE NOT EXISTS (SELECT 1 FROM uris WHERE sha2_256={sha256})""")
    	reported.foreach { r =>
    	  println(r.toString)	//DELM WTSN-39
//        sql.addBatch(
//          "uri"->r.uri.toString,
//  		    "reversedHost"->r.reversedHost,
//  		    "hierarchicalPart"->r.hierarchicalPart,
//  		    "path"->r.path,
//  		    "sha256"->r.sha256
//        )
//    	  val params: Seq[(String, ParameterValue[_])] = Seq("uri"->r.uri.toString,
//  		    "reversedHost"->r.reversedHost,
//  		    "hierarchicalPart"->r.hierarchicalPart,
//  		    "path"->r.path,
//  		    "sha256"->r.sha256)
//        sql.addBatchList(Seq(params))
      }
      val foo = reported(0)
  	  val params: Seq[Seq[(String, ParameterValue[_])]] = Seq(Seq("uri"->foo.uri.toString,
		    "reversedHost"->foo.reversedHost,
		    "hierarchicalPart"->foo.hierarchicalPart,
		    "path"->foo.path,
		    "sha256"->foo.sha256))
		  val wtf = (sql.asBatch /: params) { (s, p) =>
        s.addBatchList(Seq(p))
      }
//      sql.addBatchList(Seq(params))      
//      println(sql.sql)
//      val bar = sql.execute()
      val bar = wtf.execute()
      println(bar.length)
      0	//TODO WTSN-39
  	} catch {
  	  case e: PSQLException => if (PostgreSql.isNotDupeError(e.getMessage)) {
  	    Logger.error(e.getMessage)
  	  }
  	  0
  	}
  	println("inserted:\t"+inserted)	//DELME WTSN-39
//		return inserted
		return 0
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
  
  def findOrCreate(uris: List[String]): List[Uri] = {
    Logger.debug("Beginning bulk find or create for "+uris.size+" URIs")	//DELME WTSN-39
    val reported = uris.map { uri =>
	    try {
	      Some(new ReportedUri(uri))
	    } catch {
	      case e: URISyntaxException => Logger.warn("Invalid URI: '"+uri+"'\t"+e.getMessage)
	      None
	    }
  	}.flatten
  	Logger.debug("Writing batch of "+reported.size+" URIs")	//DELME WTSN-39
  	val writes = create(reported)
  	Logger.debug(writes+" successful write attempts")	//DELME WTSN-39
  	//TODO WTSN-39 return bulk find results
  	val found = 0
  	Logger.debug(found+" uris found")	//DELME WTSN-39
    List()	//DELME WTSN-39
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
  
  lazy val path = uri.getRawPath
  lazy val query = uri.getRawQuery
  lazy val hierarchicalPart = uri.getRawAuthority + uri.getRawPath
  lazy val reversedHost = Host.reverse(uri.getHost)
  lazy val sha256 = Hash.sha256(uri.toString).getOrElse("")
  
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