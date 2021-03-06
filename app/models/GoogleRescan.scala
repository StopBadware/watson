package models

import java.util.Date
import java.sql.Timestamp
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.Logger
import org.postgresql.util.PSQLException
import controllers.PostgreSql
import scala.util.Try

case class GoogleRescan(
    id: Int,
    uriId: Int,
    relatedUriId: Option[Int],
    status: String,
    requestedVia: String,
    rescannedAt: Long
		) {
  
  def delete(): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("DELETE FROM google_rescans WHERE id={id}").on("id"->id).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
}

object GoogleRescan {

  def create(
    uriId: Int,
    relatedUriId: Option[Int],
    status: String,
    requestedVia: String,
    rescannedAt: Long): Boolean = DB.withTransaction { implicit conn =>
	    val inserted = try {
	      SQL("""INSERT INTO google_rescans (uri_id, related_uri_id, status, requested_via, rescanned_at) 
	        SELECT {uriId}, {relatedUriId}, {status}, {requestedVia}, {rescannedAt} WHERE NOT EXISTS (SELECT 1 FROM 
	        google_rescans WHERE uri_id={uriId} AND related_uri_id"""+
	        (if (relatedUriId.isDefined) "={relatedUriId}" else " IS NULL")+
	        " AND rescanned_at={rescannedAt})").on(
	          "uriId" -> uriId,
	          "relatedUriId" -> relatedUriId,
	          "status" -> status,
	          "requestedVia" -> requestedVia,
	          "rescannedAt" -> new Timestamp(rescannedAt * 1000)).executeUpdate()
	    } catch {
	      case e: PSQLException => if (PostgreSql.isNotDupeError(e.getMessage)) {
	  	    Logger.error(e.getMessage)
	  	  }
	      0
	    }
	    return inserted > 0
    }
  
  def findByUri(uriId: Int): List[GoogleRescan] = DB.withConnection { implicit conn =>
    val rs = SQL("SELECT * FROM google_rescans WHERE uri_id={uriId}").on("uriId" -> uriId)
    return rs().map(mapFromRow).flatten.toList
  }
  
  def timeOfLast: Long = DB.withConnection { implicit conn =>
    return SQL("SELECT rescanned_at FROM google_rescans ORDER BY rescanned_at DESC LIMIT 1")()
  		.map(_[Date]("rescanned_at").getTime / 1000).headOption.getOrElse(0)
  }
  
  private def mapFromRow(row: SqlRow): Option[GoogleRescan] = {
    return Try {
	    GoogleRescan(
		    row[Int]("id"),
		    row[Int]("uri_id"),
		    row[Option[Int]]("related_uri_id"),
		    row[String]("status"),
		    row[String]("requested_via"),
		    row[Date]("rescanned_at").getTime / 1000
  		)
    }.toOption
  }  
  
}