package models

import java.sql.{BatchUpdateException, Timestamp}
import scala.util.Try
import java.util.Date
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

  def delete() = DB.withConnection { implicit conn =>
    try {
      SQL("DELETE FROM blacklist_events WHERE id={id}").on("id"->id).executeUpdate()
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
    }
  }
  
  def removeFromBlacklist(time: Long): Boolean = DB.withConnection { implicit conn =>
    val clean = ReportedEvent(uriId, source, time, Some(time))
    return BlacklistEvent.update(clean, this) > 0
  }  
  
}

object BlacklistEvent {
  
  private val BatchSize = Try(sys.env("SQLBATCH_SIZE").toInt).getOrElse(5000)
  
  def timeOfLast(source: Source): Long = DB.withConnection { implicit conn =>
    val sql = SQL("""SELECT blacklisted_at FROM blacklist_events WHERE 
        source={source}::SOURCE ORDER BY blacklisted_at desc LIMIT 1""").on("source" -> source.abbr)
    return (sql().map(_[Date]("blacklisted_at").getTime / 1000).headOption).getOrElse(0)
  }
  
  def createOrUpdate(reported: ReportedEvent): Boolean = DB.withConnection { implicit conn =>
    val events = findEventsByUri(reported.uriId, Some(reported.source), true)
    return events.size match {
      case 0 => create(List(reported)) == 1
      case 1 => update(reported, events.head) == 1
      case _ => {
        Logger.error(reported.source+": "+events.size+" currently blacklisted rows for "+reported.uriId)
        false
      }
    }
  }
  
  def createOrUpdate(reported: List[ReportedEvent], source: Source): Int = DB.withConnection { implicit conn =>
    val reportedEvents = reported.foldLeft(Map.empty[Int, ReportedEvent]) { (map, event) =>
      map ++ Map(event.uriId -> event)
    }
    val toUpdate = findEventsByUris(reportedEvents.keys.toList, source, true).map(event => (reportedEvents(event.uriId), event))
    val updated = update(toUpdate)
    Logger.info("Updated "+updated+" BlacklistEvents")
    val created = create(reported)
    Logger.info("Created "+created+" BlacklistEvents")
    return (created + updated)
  }  
  
  def markNoLongerBlacklisted(uriId: Int, source: Source, time: Long): Boolean = DB.withConnection { implicit conn =>
    val events = findEventsByUri(uriId, Some(source), true)
    val clean = ReportedEvent(uriId, source, time, Some(time))
    return events.foldLeft(0){ (ctr, event) =>
      ctr + update(clean, event)
    } == events.size
  }
  
  def updateNoLongerBlacklisted(newUris: Set[Int], source: Source, time: Long): Int = DB.withConnection { implicit conn =>
    val toBeUnblacklisted = blacklistedUriIds(time, Some(source)).filterNot(newUris.contains(_)).toList
    val table = "temp_unblacklist_" + (System.currentTimeMillis / 1000)
    return try {
      SQL("CREATE TEMP TABLE "+table+" (uri_id INTEGER PRIMARY KEY)").execute()
	    toBeUnblacklisted.grouped(BatchSize).foreach { group =>
		    val sql = "INSERT INTO "+table+" VALUES (?)" + (",(?)"*(group.size-1))
		  	val ps = conn.prepareStatement(sql)
	      for (i <- 1 to group.size) {
	        ps.setInt(i, group(i-1))
	      }
		    ps.executeUpdate()
	    }
      SQL("""UPDATE blacklist_events AS a SET blacklisted=false, unblacklisted_at=NOW() FROM temp_unblacklist 
          LEFT JOIN blacklist_events AS b ON temp_unblacklist.uri_id=b.uri_id WHERE b.source={source}::SOURCE 
          AND b.blacklisted=true{source}::SOURCE""").on("source"->source.abbr).executeUpdate()
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      0
    }
  }
  
  def blacklistedUriIds(before: Long, source: Option[Source]=None): Set[Int] = DB.withConnection { implicit conn =>
    Logger.debug("QUERYING BLACKLIST EVENTS "+Runtime.getRuntime.freeMemory) //DELME WTSN-42
    val base = "SELECT uri_id FROM blacklist_events WHERE blacklisted=true AND blacklisted_at<to_timestamp({before})"
    val rs = if (source.isDefined) {
    	SQL(base+" AND source={source}::SOURCE").on("before"->before, "source"->source.get.abbr)
    } else {
      SQL(base).on("before"->before)
    }
    Logger.debug("MAPPING BLACKLIST EVENTS "+Runtime.getRuntime.freeMemory) //DELME WTSN-42
    return rs().map(_[Int]("uri_id")).toSet
    
  }  
  
  def findByUri(uriId: Int, source: Option[Source]=None): List[BlacklistEvent] = DB.withConnection { implicit conn =>
    return findEventsByUri(uriId, source)
  }
  
  def findBlacklistedByUri(uriId: Int, source: Option[Source]=None): List[BlacklistEvent] = DB.withConnection { implicit conn =>
    return findEventsByUri(uriId, source, true)
  }  
  
  private def create(reported: List[ReportedEvent]): Int = DB.withTransaction { implicit conn =>
    return try {
    	val sql = """INSERT INTO blacklist_events (uri_id, source, blacklisted, blacklisted_at, unblacklisted_at)  
    		SELECT ?, ?::SOURCE, ?, ?, ? WHERE NOT EXISTS (SELECT 1 FROM blacklist_events 
    	  WHERE uri_id=? AND source=?::SOURCE AND blacklisted_at=?)"""
   	  val ps = conn.prepareStatement(sql)
   	  reported.grouped(BatchSize).foldLeft(0) { (total, group) =>
   	    group.foreach { reported =>
   	      val blacklistedAt = new Timestamp(reported.blacklistedAt * 1000)
   	      val unblacklistedAt = if (reported.unblacklistedAt.isDefined) {
	          new Timestamp(reported.unblacklistedAt.get * 1000)
	        } else {
	          null
	        }
   	      ps.setInt(1, reported.uriId)
   	      ps.setString(2, reported.source.abbr)
   	      ps.setBoolean(3, reported.unblacklistedAt.isEmpty)
   	      ps.setTimestamp(4, blacklistedAt)
   	      ps.setTimestamp(5, unblacklistedAt)
   	      ps.setInt(6, reported.uriId)
   	      ps.setString(7, reported.source.abbr)
   	      ps.setTimestamp(8, blacklistedAt)
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
  
  private def update(reported: ReportedEvent, event: BlacklistEvent): Int = DB.withTransaction { implicit conn =>
    return update(List((reported, event)))
  }
  
  private def update(events: List[(ReportedEvent, BlacklistEvent)]): Int = DB.withTransaction { implicit conn =>
    return try {
	    val sql = "UPDATE blacklist_events SET blacklisted=?, blacklisted_at=?, unblacklisted_at=? WHERE id=?"    
	    val ps = conn.prepareStatement(sql)
	    events.grouped(BatchSize).foldLeft(0) { (total, group) =>
	      group.foreach { eventPair =>
	        val repEvent = eventPair._1
	        val blEvent = eventPair._2
	        val blacklistedAt = Math.min(repEvent.blacklistedAt, blEvent.blacklistedAt)
	        val unblacklistedAt = if (blEvent.unblacklistedAt.isDefined && repEvent.unblacklistedAt.isEmpty) {
	          Some(new Timestamp(blEvent.unblacklistedAt.get * 1000))
	        } else if (repEvent.unblacklistedAt.isDefined) {
	          Some(new Timestamp(repEvent.unblacklistedAt.get * 1000))
	        } else {
	          None
	        }
	        ps.setBoolean(1, unblacklistedAt.isEmpty)
	        ps.setTimestamp(2, new Timestamp(blacklistedAt * 1000))
	        ps.setTimestamp(3, unblacklistedAt.getOrElse(null))
	        ps.setInt(4, blEvent.id)
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
      currentOnly: Boolean=false): List[BlacklistEvent] = DB.withConnection { implicit conn =>
    val table = "temp_import_" + (System.currentTimeMillis / 1000)
    return try {
	    SQL("CREATE TEMP TABLE "+table+" (uri_id INTEGER PRIMARY KEY)").execute()
	    uriIds.grouped(BatchSize).foreach { group =>
		    val sql = "INSERT INTO "+table+" VALUES (?)" + (",(?)"*(group.size-1))
		  	val ps = conn.prepareStatement(sql)
	      for (i <- 1 to group.size) {
	        ps.setInt(i, group(i-1))
	      }
		    ps.executeUpdate()
	    }
	    val qry = SQL("SELECT blacklist_events.* FROM "+table+" AS t LEFT JOIN blacklist_events ON "+ 
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