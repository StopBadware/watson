package models

import java.sql.{BatchUpdateException, Timestamp}
import scala.util.Try
import java.util.Date
import java.sql.Timestamp
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.Logger
import org.postgresql.util.PSQLException
import controllers.{Host, PostgreSql}
import models.enums.Source

case class BlacklistEvent(
    id: Int,
    uriId: Int,
    source: Source,
    blacklisted: Boolean,
    blacklistedAt: Long,
    unblacklistedAt: Option[Long]
    ) {

  def delete(): Boolean = DB.withConnection { implicit conn =>
    try {
      SQL("DELETE FROM blacklist_events WHERE id={id}").on("id"->id).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def removeFromBlacklist(time: Long): Boolean = BlacklistEvent.update(id, blacklistedAt, Some(time)) > 0
}

object BlacklistEvent {
  
  def currentUniqueUriCount: Int = DB.withConnection { implicit conn =>
    return Try(SQL("SELECT COUNT(DISTINCT uri_id) AS cnt FROM blacklist_events WHERE blacklisted=true")().head[Long]("cnt").toInt).getOrElse(0)
  }
  
  def timeOfLast(source: Source): Long = DB.withConnection { implicit conn =>
    val sql = SQL("""SELECT blacklisted_at FROM blacklist_events WHERE 
        source={source}::SOURCE ORDER BY blacklisted_at DESC LIMIT 1""").on("source" -> source.abbr)
    return (sql().map(_[Date]("blacklisted_at").getTime / 1000).headOption).getOrElse(0)
  }
  
  def createOrUpdate(reported: ReportedEvent): Boolean = {
    val events = findEventsByUri(reported.uriId, Some(reported.source), true)
    return events.size match {
      case 0 => create(List(reported.uriId), reported.source, reported.blacklistedAt, reported.unblacklistedAt) == 1
      case 1 => {
        val event = events.head
        val blTime = Math.min(event.blacklistedAt, reported.blacklistedAt)
        update(event.id, blTime, reported.unblacklistedAt) == 1
      }
      case _ => {
        Logger.error(reported.source+": "+events.size+" currently blacklisted rows for "+reported.uriId)
        false
      }
    }
  }
  
  def blacklistedUriIdsEventIds(source: Source, before: Option[Long]=None): Map[Int, Int] = DB.withConnection { implicit conn =>
    val base = "SELECT id, uri_id FROM blacklist_events WHERE blacklisted=true AND source={source}::SOURCE"
    val rs = if (before.isDefined) {
    	SQL(base+" AND blacklisted_at<to_timestamp({before})").on("before"->before.get, "source"->source.abbr)
    } else {
      SQL(base).on("source"->source.abbr)
    }
    return rs().map(row => (row[Int]("uri_id"), row[Int]("id"))).toMap
  }
  
  def blacklistedUriIds: Set[Int] = DB.withConnection { implicit conn =>
    return Try(SQL("SELECT uri_id FROM blacklist_events WHERE blacklisted=true")().map(_[Int]("uri_id")).toSet).getOrElse(Set())
  }
  
  def blacklistedHosts: List[String] = DB.withConnection { implicit conn =>
    return Try(SQL("""SELECT reversed_host FROM blacklist_events JOIN uris ON blacklist_events.uri_id=uris.id 
      WHERE blacklisted=true""")().map(_[String]("reversed_host")).toList).getOrElse(List()).map(Host.reverse)
  } 
  
  def find(id: Int): Option[BlacklistEvent] = DB.withConnection { implicit conn =>
    return Try(mapFromRow(SQL("SELECT * FROM blacklist_events WHERE id={id}").on("id" -> id)().head)).getOrElse(None)
  }
  
  def findByUri(uriId: Int, source: Option[Source]=None): List[BlacklistEvent] = findEventsByUri(uriId, source)
  
  def findBlacklistedByUri(uriId: Int, source: Option[Source]=None): List[BlacklistEvent] = {
    return findEventsByUri(uriId, source, true)
  }  
  
  def create(uriIds: List[Int], source: Source, blacklistedAt: Long, unblacklistedAt: Option[Long]): Int = DB.withTransaction { implicit conn =>
    val blTime = new Timestamp(blacklistedAt * 1000)
    val unblTime = if (unblacklistedAt.isDefined) new Timestamp(unblacklistedAt.get * 1000) else null
    return try {
      val sql = """INSERT INTO blacklist_events (uri_id, source, blacklisted, blacklisted_at, unblacklisted_at)  
    		SELECT ?, ?::SOURCE, ?, ?, ? WHERE NOT EXISTS (SELECT 1 FROM blacklist_events 
    	  WHERE uri_id=? AND source=?::SOURCE AND (blacklisted_at=? OR (blacklisted=true AND ?=true)))"""
   	  val ps = conn.prepareStatement(sql)
   	  urisNotBlacklisted(uriIds, source).grouped(PostgreSql.batchSize).foldLeft(0) { (total, group) =>
   	    group.foreach { id =>
   	      ps.setInt(1, id)
   	      ps.setString(2, source.abbr)
   	      ps.setBoolean(3, unblacklistedAt.isEmpty)
   	      ps.setTimestamp(4, blTime)
   	      ps.setTimestamp(5, unblTime)
   	      ps.setInt(6, id)
   	      ps.setString(7, source.abbr)
   	      ps.setTimestamp(8, blTime)
   	      ps.setBoolean(9, unblacklistedAt.isEmpty)
   	      ps.addBatch()
   	    }
   	    val batch = ps.executeBatch()
	  		ps.clearBatch()
	  		total + batch.foldLeft(0)((cnt, b) => cnt + b)
   		}
    } catch {
    	case e: PSQLException => Logger.error(e.getMessage)
      0
    }
  }
  
  private def urisNotBlacklisted(uriIds: List[Int], source: Source): List[Int] = DB.withTransaction { implicit conn =>
    val blacklisted = SQL("SELECT uri_id FROM blacklist_events WHERE blacklisted=true AND source={source}::SOURCE")
    	.on("source" -> source.abbr)().map(_[Int]("uri_id")).toSet
    return uriIds.filterNot(blacklisted.contains(_))
  }
  
  def update(eventId: Int, blacklistedAt: Long, unblacklistedAt: Option[Long]): Int = {
    return update(Set(eventId), blacklistedAt, unblacklistedAt)
  }
  
  def update(eventIds: Set[Int], blacklistedAt: Long, unblacklistedAt: Option[Long]=None): Int = DB.withTransaction { implicit conn =>
  	val blTime = new Timestamp(blacklistedAt * 1000)
  	val unblTime = if (unblacklistedAt.isDefined) new Timestamp(unblacklistedAt.get * 1000) else null
    return try {
	    val sql = """UPDATE blacklist_events SET blacklisted=?, blacklisted_at=?, unblacklisted_at=? WHERE id=? 
	      AND blacklisted_at>=?"""    
	    val ps = conn.prepareStatement(sql)
	    eventIds.grouped(PostgreSql.batchSize).foldLeft(0) { (total, group) =>
	      group.foreach { id =>
	        ps.setBoolean(1, unblacklistedAt.isEmpty)
	        ps.setTimestamp(2, blTime)
	        ps.setTimestamp(3, unblTime)
	        ps.setInt(4, id)
	        ps.setTimestamp(5, blTime)
	        ps.addBatch()
	      }
	      val batch = ps.executeBatch()
	  		ps.clearBatch()
	  		total + batch.foldLeft(0)((cnt, b) => cnt + b)
	    }
    } catch {
      case t: Throwable => t match {
	    	case e: PSQLException => Logger.error(e.getMessage)
	    	case e: BatchUpdateException => {
	    	  Logger.error(e.getMessage)
	    	  Logger.error(e.getNextException.getMessage)
	    	}
    	}
      0
    }
  }
  
  def updateBlacklistTime(uriIds: List[Int], blacklistedAt: Long, source: Source): Int = DB.withTransaction { implicit conn =>
  	val blTime = new Timestamp(blacklistedAt * 1000)
    return uriIds.grouped(100000).foldLeft(0) { (total, group) =>
	    val eventIds = findIdsByUris(group, source)
	    total + (try {
		    val sql = "UPDATE blacklist_events SET blacklisted_at=? WHERE id=? AND blacklisted_at>?"    
		    val ps = conn.prepareStatement(sql)
		    eventIds.grouped(PostgreSql.batchSize).foldLeft(0) { (ctr, events) =>
		      events.foreach { id =>
		        ps.setTimestamp(1, blTime)
		        ps.setInt(2, id)
		        ps.setTimestamp(3, blTime)
		        ps.addBatch()
		      }
		      val batch = ps.executeBatch()
		  		ps.clearBatch()
		  		ctr + batch.foldLeft(0)((cnt, b) => cnt + b)
		    }
	    } catch {
	      case t: Throwable => t match {
		    	case e: PSQLException => Logger.error(e.getMessage)
		    	case e: BatchUpdateException => {
		    	  Logger.error(e.getMessage)
		    	  Logger.error(e.getNextException.getMessage)
		    	}
	    	}
	      0
	    })
    }
  }
  
  private def findIdsByUris(uriIds: List[Int], source: Source): List[Int] = DB.withTransaction { implicit conn =>
    return try {
      SQL("DROP TABLE IF EXISTS temp_uris").execute()
      SQL("CREATE TEMP TABLE temp_uris (uri_id INTEGER PRIMARY KEY)").execute()
      uriIds.toSet.grouped(PostgreSql.batchSize).foreach { group =>
        val sql = "INSERT INTO temp_uris VALUES (?)" + (",(?)"*(group.size-1))
        val ps = conn.prepareStatement(sql)
        group.foldLeft(1) { (i, id) =>
          ps.setInt(i, id)
          i + 1
        }
        ps.executeUpdate()
      }
      SQL("""SELECT blacklist_events.id FROM blacklist_events JOIN temp_uris ON 
        blacklist_events.uri_id=temp_uris.uri_id WHERE blacklisted=true AND source={source}::SOURCE 
        LIMIT {limit}""").on("source" -> source.abbr, "limit" -> uriIds.size)().map(_[Int]("id")).toList
    } catch {
      case t: Throwable => t match {
	    	case e: PSQLException => Logger.error(e.getMessage)
	    	case e: BatchUpdateException => {
	    	  Logger.error(e.getMessage)
	    	  Logger.error(e.getNextException.getMessage)
	    	}
    	}
      List()
    }
  }      
  
  def unBlacklist(uriId: Int, source: Source, time: Long): Boolean = {
    val events = findEventsByUri(uriId, Some(source), true)
    return findEventsByUri(uriId, Some(source), true).foldLeft(0){ (ctr, event) =>
      ctr + update(event.id, event.blacklistedAt, Some(time))
    } == events.size
  }  
  
  def unBlacklist(eventIds: List[Int], unblacklistedAt: Long): Int = DB.withTransaction { implicit conn =>
    val unblTime = new Timestamp(unblacklistedAt * 1000)
    return try {
	    val sql = "UPDATE blacklist_events SET blacklisted=false, unblacklisted_at=? WHERE id=?"    
	    val ps = conn.prepareStatement(sql)
	    eventIds.grouped(PostgreSql.batchSize).foldLeft(0) { (total, group) =>
	      group.foreach { id =>
	        ps.setTimestamp(1, unblTime)
	        ps.setInt(2, id)
	        ps.addBatch()
	      }
	      val batch = ps.executeBatch()
	  		ps.clearBatch()
	  		total + batch.foldLeft(0)((cnt, b) => cnt + b)
	    }
    } catch {
      case t: Throwable => t match {
	    	case e: PSQLException => Logger.error(e.getMessage)
	    	case e: BatchUpdateException => {
	    	  Logger.error(e.getMessage)
	    	  Logger.error(e.getNextException.getMessage)
	    	}
    	}
      0
    }
  }
  
  def updateNoLongerBlacklisted(source: Source, time: Long, uris: List[Int]): Int = DB.withTransaction { implicit conn =>
    val toUnblacklist = try {
      SQL("DROP TABLE IF EXISTS temp_uris").execute()
      SQL("CREATE TEMP TABLE temp_uris (uri_id INTEGER PRIMARY KEY)").execute()
      uris.toSet.grouped(PostgreSql.batchSize).foreach { group =>
        val sql = "INSERT INTO temp_uris VALUES (?)" + (",(?)"*(group.size-1))
        val ps = conn.prepareStatement(sql)
        group.foldLeft(1) { (i, id) =>
          ps.setInt(i, id)
          i + 1
        }
        ps.executeUpdate()
      }
      val qry = SQL("""SELECT blacklist_events.id FROM blacklist_events LEFT JOIN temp_uris ON 
        blacklist_events.uri_id=temp_uris.uri_id WHERE blacklisted=true AND source={source}::SOURCE 
        AND blacklisted_at<{time} AND temp_uris.uri_id IS NULL""")
        .on("source" -> source.abbr, "time" -> new Timestamp(time*1000))()
      qry.map(_[Int]("id")).toList
    } catch {
      case t: Throwable => t match {
	    	case e: PSQLException => Logger.error(e.getMessage)
	    	case e: BatchUpdateException => {
	    	  Logger.error(e.getMessage)
	    	  Logger.error(e.getNextException.getMessage)
	    	}
    	}
      List()
    }
    return unBlacklist(toUnblacklist, time)
  }
  
  def urisBlacklistedBy(uriIds: List[Int]): Map[Int, List[Source]] = DB.withTransaction { implicit conn =>
    import Source._
    return try {
      val sql = "SELECT uri_id, source FROM blacklist_events WHERE blacklisted=true AND uri_id IN (?"+(",?"*(uriIds.size-1))+")"
      val ps = conn.prepareStatement(sql)
      uriIds.foldLeft(1) { (i, id) =>
        ps.setInt(i, id)
        i + 1
      }
      val rs = ps.executeQuery
      val sources = Set(GOOG, NSF, TTS)
      val initialMap = sources.map(_ -> Set.empty[Int]).toMap
    	val blacklisted = Iterator.continually((rs, rs.next())).takeWhile(_._2).foldLeft(initialMap) { (map, tuple) =>
    	  val id = tuple._1.getInt("uri_id")
    	  Source.withAbbr(tuple._1.getString("source")).get match {
    	    case GOOG => Map(GOOG -> map(GOOG).+(id), NSF -> map(NSF), TTS -> map(TTS))
    	    case NSF => Map(NSF -> map(NSF).+(id), GOOG -> map(GOOG), TTS -> map(TTS))
    	    case TTS => Map(TTS -> map(TTS).+(id), GOOG -> map(GOOG), NSF -> map(NSF))
    	    case _ => map
    	  }
    	}
      uriIds.map { uriId =>
        uriId -> sources.foldLeft(List.empty[Source]) { (list, source) =>
          if (blacklisted(source).contains(uriId)) source +: list else list
        }
      }.toMap
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      Map()
    }
  }
  
  private def findEventsByUri(
      uriId: Int, 
      source: Option[Source]=None,
      currentOnly: Boolean=false): List[BlacklistEvent] = DB.withConnection { implicit conn =>
    val base = "SELECT * FROM blacklist_events WHERE uri_id={uriId}" + 
      (if (currentOnly) " AND blacklisted=true" else "")
    val rs = if (source.isDefined) {
      SQL(base+" AND source={source}::SOURCE").on("uriId"->uriId, "source"->source.get.abbr)
    } else {
      SQL(base).on("uriId"->uriId)
    }
    return rs().map(mapFromRow).flatten.toList
  }
  
  private def mapFromRow(row: SqlRow): Option[BlacklistEvent] = {
    return Try {
      val unblacklistedAt = if (row[Option[Date]]("unblacklisted_at").isDefined) {
        Some(row[Option[Date]]("unblacklisted_at").get.getTime / 1000)
      } else {
        None
      }
	    BlacklistEvent(
		    row[Int]("id"),
		    row[Int]("uri_id"),
		    row[Source]("source"),
		    row[Boolean]("blacklisted"),
		    row[Date]("blacklisted_at").getTime / 1000,
		    unblacklistedAt
  		)
    }.toOption
  }
  
}

case class ReportedEvent(
    uriId: Int,
    source: Source,
    blacklistedAt: Long,
    unblacklistedAt: Option[Long]=None
    ) {
  
}