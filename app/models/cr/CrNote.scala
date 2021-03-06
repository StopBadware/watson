package models.cr

import java.util.Date
import java.sql.Timestamp
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.Logger
import org.postgresql.util.PSQLException
import scala.util.Try
import models.Note

case class CrNote(
		id: Int,
		crId: Int,
		author: Int,
		note: String,
		createdAt: Long
	) {
  
  def delete(): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("DELETE FROM cr_notes WHERE id={id}").on("id" -> id).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }

}

object CrNote {
  
  def create(crId: Int, author: Int, note: String): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("INSERT INTO cr_notes (cr_id, author, note) VALUES ({crId}, {author}, {note})").on(
      		"crId" -> crId,
      		"author" -> author,
      		"note" -> note
        ).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def find(id: Int): Option[CrNote] = DB.withConnection { implicit conn =>
    return Try(mapFromRow(SQL("SELECT * FROM cr_notes WHERE id={id} LIMIT 1").on("id"->id)().head)).getOrElse(None)
  }
  
  def findByCr(crId: Int): List[Note] = DB.withConnection { implicit conn =>
    return Try(SQL("""SELECT cr_notes.id, cr_notes.note, cr_notes.created_at, users.username FROM cr_notes 
      JOIN users ON author=users.id WHERE cr_id={crId} ORDER BY cr_notes.created_at ASC""")
      .on("crId" -> crId)().map(Note.mapFromRow).flatten.toList).getOrElse(List.empty[Note])
  }
  
  private def mapFromRow(row: SqlRow): Option[CrNote] = {
    return Try {
      CrNote(
      	row[Int]("id"), 
			  row[Int]("cr_id"),
			  row[Int]("author"),
			  row[String]("note"),
			  row[Date]("created_at").getTime / 1000
      )
    }.toOption
  }
  
}
