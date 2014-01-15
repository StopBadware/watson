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
import controllers.PostgreSql

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
  
  private val BatchSize = Try(sys.env("SQLBATCH_SIZE").toInt).getOrElse(5000)
  
  def timeOfLast(source: Source): Long = DB.withConnection { implicit conn =>
    val sql = SQL("""SELECT blacklisted_at FROM blacklist_events WHERE 
        source={source}::SOURCE ORDER BY blacklisted_at desc LIMIT 1""").on("source" -> source.abbr)
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
  
  def find(id: Int): Option[BlacklistEvent] = DB.withConnection { implicit conn =>
    return mapFromRow(SQL("SELECT * FROM blacklist_events WHERE id={id}").on("id" -> id)().head)
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
   	  uriIds.grouped(BatchSize).foldLeft(0) { (total, group) =>
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
  
  def update(eventId: Int, blacklistedAt: Long, unblacklistedAt: Option[Long]): Int = {
    return update(Set(eventId), blacklistedAt, unblacklistedAt)
  }
  
  def update(eventIds: Set[Int], blacklistedAt: Long, unblacklistedAt: Option[Long]=None): Int = DB.withTransaction { implicit conn =>
  	val blTime = new Timestamp(blacklistedAt * 1000)
  	val unblTime = if (unblacklistedAt.isDefined) Some(new Timestamp(unblacklistedAt.get * 1000)) else None
    return try {
	    val sql = """UPDATE blacklist_events SET blacklisted=?, blacklisted_at=?, unblacklisted_at=? WHERE id=? 
	      AND blacklisted_at>=?"""    
	    val ps = conn.prepareStatement(sql)
	    eventIds.grouped(BatchSize).foldLeft(0) { (total, group) =>
	      group.foreach { id =>
	        ps.setBoolean(1, unblacklistedAt.isEmpty)
	        ps.setTimestamp(2, blTime)
	        ps.setTimestamp(3, unblTime.getOrElse(null))
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
  
  def updateBlacklistTime(eventId: Int, blacklistedAt: Long): Int = {
    return updateBlacklistTime(List(eventId), blacklistedAt)
  }
  
  def updateBlacklistTime(eventIds: List[Int], blacklistedAt: Long): Int = DB.withTransaction { implicit conn =>
  	val blTime = new Timestamp(blacklistedAt * 1000)
    return try {
	    val sql = "UPDATE blacklist_events SET blacklisted_at=? WHERE id=? AND blacklisted_at>?"    
	    val ps = conn.prepareStatement(sql)
	    eventIds.grouped(BatchSize).foldLeft(0) { (total, group) =>
	      group.foreach { id =>
	        ps.setTimestamp(1, blTime)
	        ps.setInt(2, id)
	        ps.setTimestamp(3, blTime)
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
	    eventIds.grouped(BatchSize).foldLeft(0) { (total, group) =>
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
      uris.grouped(BatchSize).foreach { group =>
        val sql = "INSERT INTO temp_uris VALUES (?)" + (",(?)"*(group.size-1))
        val ps = conn.prepareStatement(sql)
        for (i <- 1 to group.size) {
          ps.setInt(i, group(i-1))
        }
        ps.executeUpdate()
      }
      val qry = SQL("""SELECT blacklist_events.id FROM blacklist_events LEFT JOIN temp_uris ON 
        blacklist_events.uri_id=temp_uris.uri_id WHERE blacklisted=true AND source={source}::SOURCE 
        AND blacklisted_at<{time} AND temp_uris.uri_id IS NULL""")
        .on("source"->source.abbr, "time"->new Timestamp(time*1000))()
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
  
  private def findEventsByUris(
      uriIds: List[Int], 
      source: Source,
      currentOnly: Boolean=false): List[BlacklistEvent] = DB.withTransaction { implicit conn =>
    return try {
      SQL("DROP TABLE IF EXISTS temp_uris").execute()
	    SQL("CREATE TEMP TABLE temp_uris (uri_id INTEGER PRIMARY KEY)").execute()
	    uriIds.grouped(BatchSize).foreach { group =>
		    val sql = "INSERT INTO temp_uris VALUES (?)" + (",(?)"*(group.size-1))
		  	val ps = conn.prepareStatement(sql)
	      for (i <- 1 to group.size) {
	        ps.setInt(i, group(i-1))
	      }
		    ps.executeUpdate()
	    }
	    val qry = SQL("SELECT blacklist_events.* FROM temp_uris AS t LEFT JOIN blacklist_events ON "+ 
	      "t.uri_id=blacklist_events.uri_id WHERE blacklist_events.source={source}::SOURCE"+
	      (if (currentOnly) " AND blacklisted=true" else "")).on("source"->source.abbr)()
	    qry.map { row =>
	      val unblacklistStamp = row[Option[Date]]("unblacklisted_at")
	      val unblacklistedAt = if (unblacklistStamp.isDefined) Some(unblacklistStamp.get.getTime / 1000) else None
	      BlacklistEvent(
			    row[Int]("id"),
			    row[Int]("uri_id"),
			    row[Source]("source"),
			    row[Boolean]("blacklisted"),
			    row[Date]("blacklisted_at").getTime / 1000,
			    unblacklistedAt)
	    }.toList
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      List()
    }
  }    
  
  private def mapFromRow(row: SqlRow): Option[BlacklistEvent] = {
    return try {
      val unblacklistedAt = if (row[Option[Date]]("unblacklisted_at").isDefined) {
        Some(row[Option[Date]]("unblacklisted_at").get.getTime / 1000)
      } else {
        None
      }
	    Some(BlacklistEvent(
		    row[Int]("id"),
		    row[Int]("uri_id"),
		    row[Source]("source"),
		    row[Boolean]("blacklisted"),
		    row[Date]("blacklisted_at").getTime / 1000,
		    unblacklistedAt
  		))
    } catch {
      case e: Exception => None
    }
  }
  
}

case class ReportedEvent(
    uriId: Int,
    source: Source,
    blacklistedAt: Long,
    unblacklistedAt: Option[Long]=None
    ) {
  
}