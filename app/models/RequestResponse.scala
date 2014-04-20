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
  
  def allResponses: List[ResponseSummary] = DB.withConnection { implicit conn =>
    return try {
      SQL("""SELECT question, questions.id AS question_id, questions.enabled AS question_enabled, answer, answers.id AS answer_id, 
        answers.enabled AS answer_enabled, COUNT(*) AS count FROM request_responses AS rr JOIN request_questions AS questions 
        ON rr.question_id=questions.id JOIN request_answers AS answers ON rr.answer_id=answers.id GROUP BY question, answer, 
        questions.enabled, answers.enabled, questions.id, answers.id ORDER BY question_enabled DESC, question ASC, count DESC""")()
        .map(ResponseSummary.mapFromRow).flatten.toList
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      List()
    }
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

case class ResponseSummary(
    question: String,
    questionId: Int, 
    questionEnabled: Boolean, 
    answer: String, 
    answerId: Int,
    answerEnabled: Boolean, 
    count: Int) {
  
}

object ResponseSummary {
  
  def mapFromRow(row: SqlRow): Option[ResponseSummary] = {
    return try {
      Some(ResponseSummary(
      	row[String]("question"),
      	row[Int]("question_id"),
			  row[Boolean]("question_enabled"),
			  row[String]("answer"),
			  row[Int]("answer_id"),
			  row[Boolean]("answer_enabled"),
			  row[Long]("count").toInt
      ))
    } catch {
      case e: Exception => None
    }
  }
  
}