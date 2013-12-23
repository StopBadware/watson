package models

import java.sql.Timestamp
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
  
  private val BatchSize = 2000
  
  def createOrUpdate(reported: ReportedEvent): Boolean = DB.withConnection { implicit conn =>
    val events = findEventsByUri(reported.uriId, Some(reported.source), true)
    return events.size match {
      case 0 => create(reported)
      case 1 => update(reported, events.head)==1
      case _ => {
        Logger.error(reported.source+": "+events.size+" currently blacklisted rows for "+reported.uriId)
        false
      }
    }
  }
  
  def createOrUpdate(reported: List[ReportedEvent], source: Source): Boolean = DB.withConnection { implicit conn =>
    val reportedEvents = reported.foldLeft(Map.empty[Int, ReportedEvent]) { (map, event) =>
    	map ++ Map(event.uriId -> event)
    }
    val uriIds = reported.map(_.uriId)
    val events = findEventsByUris(uriIds, source, true)
    //TODO WTSN-40 update existing entries if needed
    val toUpdate = events.map { event =>
//      val reportedEvent = reportedEvents(event.uriId+"-")
    }
    //TODO WTSN-40 create new entries
    return false		//TODO WTSN-40
  }  
  
  private def create(reported: ReportedEvent): Boolean = DB.withTransaction { implicit conn =>
    val inserted = try {
      SQL("""INSERT INTO blacklist_events (uri_id, source, blacklisted, blacklisted_at, unblacklisted_at) 
        SELECT {uriId}, {source}::SOURCE, {blacklisted}, {blacklistedAt}, {unblacklistedAt} 
        WHERE NOT EXISTS (SELECT 1 FROM blacklist_events 
        WHERE uri_id={uriId} AND source={source}::SOURCE AND blacklisted_at={blacklistedAt})""").on(
          "uriId" -> reported.uriId,
          "source" -> reported.source.abbr,
          "blacklistedAt" -> new Date(reported.blacklistedAt * 1000),
          "unblacklistedAt" -> {
            if (reported.unblacklistedAt.isDefined) new Date(reported.unblacklistedAt.get * 1000) else None
          },
          "blacklisted" -> reported.unblacklistedAt.isEmpty).executeUpdate()
    } catch {
      case e: PSQLException => if (PostgreSql.isNotDupeError(e.getMessage)) {
  	    Logger.error(e.getMessage)
  	  }
      0
    }
    return inserted > 0
  }
  
  private def update(reported: ReportedEvent, event: BlacklistEvent): Int = DB.withTransaction { implicit conn =>
    return try {
      SQL("""UPDATE blacklist_events SET blacklisted={blacklisted}, blacklisted_at={blacklistedAt}, 
        unblacklisted_at={unblacklistedAt} WHERE id={id}""").on(
          "id" -> event.id,
          "blacklistedAt" -> {
            val blacklistedAt = Math.min(reported.blacklistedAt, event.blacklistedAt)
            new Date(blacklistedAt * 1000)
          },
          "unblacklistedAt" -> {
            if (reported.unblacklistedAt.isDefined) new Date(reported.unblacklistedAt.get * 1000) else None
          },
          "blacklisted" -> reported.unblacklistedAt.isEmpty).executeUpdate()
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      0
    }
  }
  
  private def update(events: List[(ReportedEvent, BlacklistEvent)]): Int = DB.withTransaction { implicit conn =>
    return try {
	    val sql = "UPDATE blacklist_events SET blacklisted=?, blacklisted_at=?, unblacklisted_at=? WHERE id=?"    
	    val ps = conn.prepareStatement(sql)
	    events.grouped(BatchSize).foldLeft(0) { (total, group) =>
	      group.foreach { eventPair =>
	        val reportedEvent = eventPair._1
	        val blacklistEvent = eventPair._2
	        val blacklistedAt = Math.min(reportedEvent.blacklistedAt, blacklistEvent.blacklistedAt)
	        val unblacklistedAt = if (reportedEvent.unblacklistedAt.isDefined) {
	          new Timestamp(reportedEvent.unblacklistedAt.get * 1000)
	        } else {
	          null
	        }
	        ps.setBoolean(1, reportedEvent.unblacklistedAt.isEmpty)
	        ps.setTimestamp(2, new Timestamp(blacklistedAt * 1000))
	        ps.setTimestamp(3, unblacklistedAt)
	        ps.setInt(4, blacklistEvent.id)
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
  
  def markNoLongerBlacklisted(uriId: Int, source: Source, time: Long): Boolean = DB.withConnection { implicit conn =>
    val events = findEventsByUri(uriId, Some(source), true)
    val clean = ReportedEvent(uriId, source, time, Some(time))
    return events.foldLeft(0){ (ctr, event) =>
      ctr + update(clean, event)
    } == events.size
  }
  
  def blacklisted(source: Option[Source]=None): List[BlacklistEvent] = DB.withConnection { implicit conn =>
    val base = "SELECT * FROM blacklist_events WHERE blacklisted=true"
    val rs = if (source.isDefined) {
    	SQL(base+" AND source={source}::SOURCE").on("source"->source.get.abbr)
    } else {
      SQL(base)
    }
    return rs().map(mapFromRow).flatten.toList
  }
  
  def findByUri(uriId: Int, source: Option[Source]=None): List[BlacklistEvent] = DB.withConnection { implicit conn =>
    return findEventsByUri(uriId, source)
  }
  
  def findBlacklistedByUri(uriId: Int, source: Option[Source]=None): List[BlacklistEvent] = DB.withConnection { implicit conn =>
    return findEventsByUri(uriId, source, true)
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
    return uriIds.grouped(BatchSize).foldLeft(List.empty[BlacklistEvent]) { (list, group) =>
	    val sql = "SELECT * FROM blacklist_events WHERE source=?::SOURCE AND "+
	    	"uri_id in (?" + (",?"*(group.size-1)) + ")" + (if (currentOnly) " AND blacklisted=true" else "")
	  	val ps = conn.prepareStatement(sql)
	  	ps.setString(1, source.abbr)
      for (i <- 2 to group.size + 1) {
        ps.setInt(i, group(i-2))
      }
	    val rs = ps.executeQuery
	    Iterator.continually((rs, rs.next())).takeWhile(_._2).map { case (row, hasNext) =>
	      val unblacklistedAt = if (row.getTimestamp("unblacklisted_at") == null) {
	        None
	      } else {
	        Some(row.getTimestamp("unblacklisted_at").getTime / 1000)
	      }
	      BlacklistEvent(
			    row.getInt("id"),
			    row.getInt("uri_id"),
			    Source.withAbbr(row.getString("source")).get,
			    row.getBoolean("blacklisted"),
			    row.getTimestamp("blacklisted_at").getTime / 1000,
			    unblacklistedAt)
	    }.toList ++ list
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