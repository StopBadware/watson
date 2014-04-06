package models

import java.util.Date
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.Logger
import org.postgresql.util.PSQLException
import scala.util.Try

case class AssociatedUri(
    id: Int,
    reviewId: Int,
    uriId: Int,
    resolved: Option[Boolean],
    uriType: Option[String],
    intent: Option[String],
    associatedAt: Long
  ) {

  def delete(): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("DELETE FROM associated_uris WHERE id={id}").on("id" -> id).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def update(
      newResolved: Option[Boolean], 
      newUriType: Option[String], 
      newIntent: Option[String]): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("UPDATE associated_uris SET resolved={newResolved}, uri_type={newUriType}, intent={newIntent} WHERE id={id}")
        .on("id" -> id, "newResolved" -> newResolved, "newUriType" -> newUriType, "newIntent" -> newIntent).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
}

object AssociatedUri {
  
  def create(
      reviewId: Int,
      uriId: Int,
      resolved: Option[Boolean],
      uriType: Option[String],
      intent: Option[String]): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("""INSERT INTO associated_uris (review_id, uri_id, resolved, uri_type, intent) 
        VALUES({reviewId}, {uriId}, {resolved}, {uriType}, {intent})""")
        .on(
          "reviewId" -> reviewId, 
          "uriId" -> uriId, 
          "resolved" -> resolved, 
          "uriType" -> uriType, 
          "intent" -> intent).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def find(id: Int): Option[AssociatedUri] = DB.withConnection { implicit conn =>
    return Try(mapFromRow(SQL("SELECT * FROM associated_uris WHERE id={id} LIMIT 1").on("id"->id)().head)).getOrElse(None)
  }
  
  def findByReviewId(reviewId: Int): List[AssociatedUri] = DB.withConnection { implicit conn =>
    return Try(SQL("SELECT * FROM associated_uris WHERE review_id={reviewId} ORDER BY associated_at ASC")
      .on("reviewId" -> reviewId)().map(mapFromRow).flatten.toList).getOrElse(List.empty[AssociatedUri])
  }
  
  def findByUriId(uriId: Int): List[AssociatedUri] = DB.withConnection { implicit conn =>
    return Try(SQL("SELECT * FROM associated_uris WHERE uri_id={uriId} ORDER BY associated_at ASC")
      .on("uriId" -> uriId)().map(mapFromRow).flatten.toList).getOrElse(List.empty[AssociatedUri])
  }
  
  private def mapFromRow(row: SqlRow): Option[AssociatedUri] = {
    return try {
      Some(AssociatedUri(
      	row[Int]("id"), 
			  row[Int]("review_id"),
			  row[Int]("uri_id"),
			  row[Option[Boolean]]("resolved"),
			  row[Option[String]]("uri_type"),
			  row[Option[String]]("intent"),
			  row[Date]("associated_at").getTime / 1000
      ))
    } catch {
      case e: Exception => None
    }
  }
  
}

case class RevAssocUri(uriId: Int, resolved: Option[Boolean], uriType: Option[String],intent: Option[String])