package models

import java.util.Date
import java.sql.Timestamp
import scala.util.Try
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.Logger
import org.postgresql.util.PSQLException
import controllers.Email

case class ReviewRequest(
    id: Int,
    uriId: Int,
    open: Boolean,
    email: String,
    ip: Option[Long],
    requesterNotes: Option[String],
    requestedAt: Long,
    closedAt: Option[Long]
    ) {
  
  def delete() = DB.withConnection { implicit conn =>
    try {
      SQL("DELETE FROM review_requests WHERE id={id}").on("id" -> id).executeUpdate()
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
    }
  }
  
  def close(closedAt: Option[Long]=None): Boolean = DB.withConnection { implicit conn =>
  	val closeTime = closedAt.getOrElse(System.currentTimeMillis / 1000)
    return try {
      SQL("UPDATE review_requests SET open=false, closed_at={closedAt} WHERE id={id}")
      	.on("id" -> id, "closedAt" -> new Timestamp(closeTime * 1000)).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  } 

}

object ReviewRequest {
  
  def create(uriId: Int,
    email: String,
    ip: Option[Long]=None,
    notes: Option[String]=None): Boolean = DB.withConnection { implicit conn =>
    return try {
      val ipOrNull = if (ip.isDefined) ip.get else null
      val notesOrNull = if (notes.isDefined && notes.nonEmpty) notes.get else null
      if (Email.isValid(email)) {
	      SQL("""INSERT INTO review_requests (uri_id, email, ip, requester_notes) 
	        VALUES({uriId}, {email}, {ip}, {notes})""")
	        .on("uriId" -> uriId, "email" -> email, "ip" -> ipOrNull, "notes" -> notesOrNull)
	        .executeUpdate() > 0
      } else {
        false
      }
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def find(id: Int): Option[ReviewRequest] = DB.withConnection { implicit conn =>
    return try {
      SQL("SELECT * FROM review_requests WHERE id={id}").on("id" -> id)().map(mapFromRow).headOption.getOrElse(None)
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      None
    }
  }
  
  def findByUri(uriId: Int): List[ReviewRequest] = DB.withConnection { implicit conn =>
    return try {
      SQL("SELECT * FROM review_requests WHERE uri_id={uriId} ORDER BY requested_at ASC")
      	.on("uriId" -> uriId)().map(mapFromRow).toList.flatten
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      List()
    }
  }
  
  private def mapFromRow(row: SqlRow): Option[ReviewRequest] = {
    val closedAt = if (row[Option[Date]]("closed_at").isDefined) {
      Some(row[Option[Date]]("closed_at").get.getTime / 1000)
    } else {
      None
    }
    return try {  
	    Some(ReviewRequest(
		    row[Int]("id"),
		    row[Int]("uri_id"),
		    row[Boolean]("open"),
		    row[String]("email"),
		    row[Option[Long]]("ip"),
		    row[Option[String]]("requester_notes"),
		    row[Date]("requested_at").getTime / 1000,
		    closedAt
  		))
    } catch {
      case e: Exception => None
    }
  }   
  
}
