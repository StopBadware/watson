package models

import java.util.Date
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.Logger
import org.postgresql.util.PSQLException
import scala.util.Try

case class RequestResponse(id: Int, reviewRequestId: Int, questionId: Int, answerId: Option[Int], respondedAt: Long) {

}

object RequestResponse {
  
  def create(reviewRequestId: Int, questionId: Int, answerId: Int): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("""INSERT INTO request_responses (review_request_id, question_id, answer_id) 
        VALUES({reviewRequestId}, {questionId}, {answerId})""")
        .on("reviewRequestId" -> reviewRequestId, "questionId" -> questionId, "answerId" -> answerId).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def find(id: Int): Option[RequestResponse] = DB.withConnection { implicit conn =>
    return Try(mapFromRow(SQL("SELECT * FROM request_responses WHERE id={id} LIMIT 1").on("id"->id)().head)).getOrElse(None)
  }
  
  def findByQuestion(questionId: Int): List[RequestResponse] = DB.withConnection { implicit conn =>
    return Try(SQL("SELECT * FROM request_responses WHERE question_id={questionId}").on("questionId" -> questionId)()
      .map(mapFromRow).flatten.toList).getOrElse(List())
  }
  
  def findByReview(reviewId: Int): List[RequestResponse] = DB.withConnection { implicit conn =>
    return Try(SQL("SELECT * FROM request_responses WHERE review_request_id={reviewId}").on("reviewId" -> reviewId)()
      .map(mapFromRow).flatten.toList).getOrElse(List())
  }
  
  private def mapFromRow(row: SqlRow): Option[RequestResponse] = {
    return try {
      Some(RequestResponse(
      	row[Int]("id"), 
			  row[Int]("review_request_id"),
			  row[Int]("question_id"),
			  row[Option[Int]]("answer_id"),
			  row[Date]("responded_at").getTime / 1000
      ))
    } catch {
      case e: Exception => None
    }
  }
  
}