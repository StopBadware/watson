package models

import java.util.Date
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.Logger
import org.postgresql.util.PSQLException
import scala.util.Try

case class RequestAnswer(id: Int, questionId: Int, answer: String, enabled: Boolean, createdAt: Long) {
  
  def disable: Boolean = toggle(false)
  
  def enable: Boolean = toggle(true)
  
  private def toggle(enabled: Boolean): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("UPDATE request_answers SET enabled={enabled} WHERE id={id}")
      	.on("id" -> id, "enabled" -> enabled).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def update(newText: String): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("UPDATE request_answers SET answer={newText} WHERE id={id}")
        .on("id" -> id, "newText" -> newText).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }

}

object RequestAnswer {
  
  def create(answer: String, questionId: Int): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("INSERT INTO request_answers (answer, question_id) VALUES({answer}, {questionId})")
        .on("answer" -> answer, "questionId" -> questionId).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def find(id: Int): Option[RequestAnswer] = DB.withConnection { implicit conn =>
    return Try(mapFromRow(SQL("SELECT * FROM request_answers WHERE id={id} LIMIT 1").on("id"->id)().head)).getOrElse(None)
  }
  
  def mapFromRow(row: SqlRow): Option[RequestAnswer] = {
    return try {
      Some(RequestAnswer(
      	row[Int]("id"),
      	row[Int]("question_id"),
      	row[String]("answer"),
      	row[Boolean]("enabled"),
			  row[Date]("created_at").getTime / 1000
      ))
    } catch {
      case e: Exception => None
    }
  }
  
}