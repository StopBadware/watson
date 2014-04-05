package models

import java.util.Date
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.Logger
import org.postgresql.util.PSQLException
import scala.util.Try

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
  
  def update(code: Option[String], hash: Option[String]): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("UPDATE review_code SET bad_code={badCode}, exec_sha2_256={sha256}, updated_at=NOW() WHERE id={id}")
        .on("id" -> id, "badCode" -> code, "sha256" -> hash).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }

}

object ReviewCode {
  
  def create(reviewId: Int, badCode: Option[String], sha256: Option[String]): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("INSERT INTO review_code (review_id, bad_code, exec_sha2_256) VALUES({reviewId}, {badCode}, {sha256})")
        .on("reviewId" -> reviewId, "badCode" -> badCode, "sha256" -> sha256).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def find(id: Int): Option[ReviewCode] = DB.withConnection { implicit conn =>
    return Try(mapFromRow(SQL("SELECT * FROM review_code WHERE id={id} LIMIT 1").on("id"->id)().head)).getOrElse(None)
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