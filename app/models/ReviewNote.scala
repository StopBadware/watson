package models

import java.util.Date
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.Logger
import org.postgresql.util.PSQLException
import scala.util.Try

case class ReviewNote(
    id: Int,
    reviewId: Int,
    authorId: Int,
    note: String,
    createdAt: Long
  ) {
  
  
  def delete(): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("DELETE FROM review_notes WHERE id={id}").on("id" -> id).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }  

}

object ReviewNote {
  
  def create(reviewId: Int, authorId: Int, note: String): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("INSERT INTO review_notes (review_id, author, note) VALUES({reviewId}, {authorId}, {note})")
        .on("reviewId" -> reviewId, "authorId" -> authorId, "note" -> note).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def find(id: Int): Option[ReviewNote] = DB.withConnection { implicit conn =>
    return Try(mapFromRow(SQL("SELECT * FROM review_notes WHERE id={id} LIMIT 1").on("id"->id)().head)).getOrElse(None)
  }
  
  def findByAuthor(authorId: Int): List[ReviewNote] = DB.withConnection { implicit conn =>
    Try(SQL("SELECT * FROM review_notes WHERE author = {authorId}")
    		.on("authorId" -> authorId)().map(mapFromRow).flatten.toList).getOrElse(List.empty[ReviewNote])
  }
  
  def findByReview(reviewId: Int): List[Note] = DB.withConnection { implicit conn =>
    return try {
      SQL("""SELECT review_notes.id, username, note, review_notes.created_at FROM review_notes JOIN users ON 
        author=users.id WHERE review_id={reviewId} ORDER BY review_notes.created_at ASC""")
        .on("reviewId" -> reviewId)().map(Note.mapFromRow).flatten.toList
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      List()
    }
  }
  
  private def mapFromRow(row: SqlRow): Option[ReviewNote] = {
    return Try {
      ReviewNote(
      	row[Int]("id"), 
			  row[Int]("review_id"),
			  row[Int]("author"),
			  row[String]("note"),
			  row[Date]("created_at").getTime / 1000
      )
    }.toOption
  }
  
}