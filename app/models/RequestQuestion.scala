package models

import java.util.Date
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.Logger
import org.postgresql.util.PSQLException
import scala.util.Try

case class RequestQuestion(id: Int, question: String, enabled: Boolean, createdAt: Long) {
  
  def disable: Boolean = toggle(false)
  
  def enable: Boolean = toggle(true)
  
  private def toggle(enabled: Boolean): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("UPDATE request_questions SET enabled={enabled} WHERE id={id}")
      	.on("id" -> id, "enabled" -> enabled).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def update(newText: String): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("UPDATE request_questions SET question={newText} WHERE id={id}")
        .on("id" -> id, "newText" -> newText).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def answers: List[RequestAnswer] = DB.withConnection { implicit conn =>
    return Try(SQL("SELECT * FROM request_answers WHERE question_id={id}").on("id" -> id)()
      .map(RequestAnswer.mapFromRow).flatten.toList).getOrElse(List())
  }
  
  def enabledAnswers: List[RequestAnswer] = DB.withConnection { implicit conn =>
    return Try(SQL("SELECT * FROM request_answers WHERE question_id={id} AND enabled=true").on("id" -> id)()
      .map(RequestAnswer.mapFromRow).flatten.toList).getOrElse(List())
  }
  
}

object RequestQuestion {
  
  def create(question: String): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("INSERT INTO request_questions (question) VALUES({question})")
        .on("question" -> question).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def find(id: Int): Option[RequestQuestion] = DB.withConnection { implicit conn =>
    return Try(mapFromRow(SQL("SELECT * FROM request_questions WHERE id={id} LIMIT 1").on("id"->id)().head)).getOrElse(None)
  }
  
  def findByText(text: String): Option[RequestQuestion] = DB.withConnection { implicit conn =>
    return Try(mapFromRow(SQL("SELECT * FROM request_questions WHERE question={text} LIMIT 1").on("text"->text)().head)).getOrElse(None)
  }
  
  def enabled: List[RequestQuestion] = DB.withConnection { implicit conn =>
    return Try(SQL("SELECT * FROM request_questions WHERE enabled=true")().map(mapFromRow).flatten.toList).getOrElse(List())
  }
  
  def all: List[RequestQuestion] = DB.withConnection { implicit conn =>
    return Try(SQL("SELECT * FROM request_questions ORDER BY enabled DESC, created_at DESC")().map(mapFromRow).flatten.toList).getOrElse(List())
  }
  
  private def mapFromRow(row: SqlRow): Option[RequestQuestion] = {
    return try {
      Some(RequestQuestion(
      	row[Int]("id"), 
			  row[String]("question"),
			  row[Boolean]("enabled"),
			  row[Date]("created_at").getTime / 1000
      ))
    } catch {
      case e: Exception => None
    }
  }
  
}