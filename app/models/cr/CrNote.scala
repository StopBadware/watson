package models.cr

import java.util.Date
import java.sql.Timestamp
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.Logger
import org.postgresql.util.PSQLException
import scala.util.Try

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
  
  def findByCr(crId: Int): List[CrNote] = DB.withConnection { implicit conn =>
    return Try(SQL("SELECT * FROM cr_notes WHERE cr_id={crId}")
      .on("crId" -> crId)().map(mapFromRow).flatten.toList).getOrElse(List.empty[CrNote])
  }
  
  private def mapFromRow(row: SqlRow): Option[CrNote] = {
    return try {
      Some(CrNote(
      	row[Int]("id"), 
			  row[Int]("cr_id"),
			  row[Int]("author"),
			  row[String]("note"),
			  row[Date]("created_at").getTime / 1000
      ))
    } catch {
      case e: Exception => None
    }
  }
  
}