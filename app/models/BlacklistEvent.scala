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

}

object BlacklistEvent {
  
  def create(reported: ReportedEvent): Boolean = DB.withConnection { implicit conn =>
    val inserted = try {
      SQL("""INSERT INTO blacklist_events (uri_id, source, blacklisted, blacklisted_at, unblacklisted_at) 
          SELECT {uriId}, {source}::SOURCE, {blacklisted}, {blacklistedAt}, {unblacklistedAt} 
          WHERE NOT EXISTS (SELECT 1 FROM blacklist_events 
          WHERE uri_id={uriId} AND source={source}::SOURCE AND blacklisted_at={blacklistedAt})""").on(
            "uriId"->reported.uriId,
            "source"->reported.source.abbr,
            "blacklistedAt"->new Date(reported.blacklistedAt * 1000),
            "unblacklistedAt"-> {
              if (reported.unblacklistedAt.isDefined) new Date(reported.unblacklistedAt.get * 1000) else None
            },
            "blacklisted"->reported.unblacklistedAt.isEmpty
          ).executeUpdate()
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      0
    }
    return inserted > 0
  }
  
  def findByUri(uriId: Int): List[BlacklistEvent] = DB.withConnection { implicit conn =>
    val rs = SQL("SELECT * FROM blacklist_events WHERE uri_id={uriId}").on("uriId"->uriId)
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