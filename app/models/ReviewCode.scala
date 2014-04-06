package models

import java.util.Date
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.Logger
import org.postgresql.util.PSQLException
import scala.util.Try
import controllers.PostgreSql

case class ReviewCode(
    id: Int,
    reviewId: Int,
    badCode: Option[String],
    execSha256: Option[String],
    createdAt: Long,
    updatedAt: Long
  ) {
  
  def delete(): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("DELETE FROM review_code WHERE id={id}").on("id" -> id).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def update(newBadCode: Option[String], newSha256: Option[String]): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("UPDATE review_code SET bad_code={badCode}, exec_sha2_256={sha256}, updated_at=NOW() WHERE id={id}")
        .on("id" -> id, "badCode" -> newBadCode, "sha256" -> newSha256).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }

}

object ReviewCode {
  
  def create(reviewId: Int, badCode: Option[String], sha256: Option[String]): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("""INSERT INTO review_code (review_id, bad_code, exec_sha2_256) SELECT {reviewId}, {badCode}, {sha256} 
          WHERE NOT EXISTS (SELECT 1 FROM review_code WHERE review_id={reviewId})""")
        .on("reviewId" -> reviewId, "badCode" -> badCode, "sha256" -> sha256).executeUpdate() > 0
    } catch {
      case e: PSQLException => if (PostgreSql.isNotDupeError(e.getMessage)) {
  	    Logger.error(e.getMessage)
  	  }
      false
    }
  }
  
  def createOrUpdate(reviewId: Int, badCode: Option[String], sha256: Option[String]): Boolean = {
    val existing = findByReview(reviewId)
    return if (existing.isDefined) {
      existing.get.update(badCode, sha256)
    } else {
      create(reviewId, badCode, sha256)
    }
  } 
  
  def find(id: Int): Option[ReviewCode] = DB.withConnection { implicit conn =>
    return Try(mapFromRow(SQL("SELECT * FROM review_code WHERE id={id} LIMIT 1").on("id"->id)().head)).getOrElse(None)
  }
  
  def findByReview(reviewId: Int): Option[ReviewCode] = DB.withConnection { implicit conn =>
    return Try(mapFromRow(SQL("SELECT * FROM review_code WHERE review_id={reviewId} ORDER BY created_at DESC")
      .on("reviewId"->reviewId)().head).get).toOption
  }
  
  def findByExecutable(sha256: String): List[ReviewCode] = DB.withConnection { implicit conn =>
    return Try(SQL("SELECT * FROM review_code WHERE exec_sha2_256={sha256} ORDER BY created_at DESC")
      .on("sha256"->sha256)().map(mapFromRow).flatten.toList).getOrElse(List.empty[ReviewCode])
  }
  
  private def mapFromRow(row: SqlRow): Option[ReviewCode] = {
    return try {
      Some(ReviewCode(
      	row[Int]("id"), 
			  row[Int]("review_id"),
			  row[Option[String]]("bad_code"),
			  row[Option[String]]("exec_sha2_256"),
			  row[Date]("created_at").getTime / 1000,
			  row[Date]("updated_at").getTime / 1000
      ))
    } catch {
      case e: Exception => None
    }
  }
  
}