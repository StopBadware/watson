package models

import java.util.Date
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.Logger
import org.postgresql.util.PSQLException

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
            "blacklisted" -> reported.unblacklistedAt.isEmpty
          ).executeUpdate()
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      0
    }
    return inserted > 0
  }
  
  private def update(reported: ReportedEvent, event: BlacklistEvent): Int = DB.withTransaction { implicit conn =>
    val updated = try {
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
            "blacklisted" -> reported.unblacklistedAt.isEmpty
          ).executeUpdate()
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      0
    }
    return updated
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
      case e: Exception => println(e) 
        None
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