package models

import java.util.Date
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.Logger
import org.postgresql.util.PSQLException
import scala.util.Try
import models.enums.ReviewStatus
import controllers.PostgreSql.rowToIntArray

case class Review(
	  id: Int, 
	  uriId: Int,
	  reviewedBy: Option[Int],
	  verifiedBy: Option[Int],
	  reviewDataId: Option[Int],
	  reviewTags: List[Int],
	  status: ReviewStatus,
	  createdAt: Long,
	  statusUpdatedAt: Long
  ) {
  
  def reviewed(verdict: ReviewStatus, reviewer: Int, reviewData: Option[Int]=None): Boolean = DB.withConnection { implicit conn =>
    val dataId = if (reviewData.isDefined) reviewData else reviewDataId
    val newStatus = if (verdict == ReviewStatus.BAD) ReviewStatus.PENDING else verdict
    return try {
      SQL("""UPDATE reviews SET status={status}::REVIEW_STATUS, reviewed_by={reviewerId}, 
        review_data_id={dataId}, status_updated_at=NOW() WHERE id={id}""")
        .on("id" -> id, "status" -> newStatus.toString, "reviewerId" -> reviewer, "dataId" -> dataId).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def reject(verifier: Int, comments: String): Boolean = DB.withConnection { implicit conn =>
    //TODO WTSN-18 add comments to review data
    return close(ReviewStatus.REJECTED, Some(verifier))
  }
  
  def close(closeAs: ReviewStatus, verifier: Option[Int]=None): Boolean = DB.withConnection { implicit conn =>
    val verifierId = if (verifier.isDefined) verifier else verifiedBy
    val closed = try {
      SQL("""UPDATE reviews SET status={status}::REVIEW_STATUS, verified_by={verifierId}, 
        status_updated_at=NOW() WHERE id={id} AND status<='PENDING'::REVIEW_STATUS""")
        .on("id" -> id, "status" -> closeAs.toString, "verifierId" -> verifierId).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
    if (closed) {
      //TODO WTSN-31 close review requests
    }
    return closed
  }
  
  def delete(): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("DELETE FROM reviews WHERE id={id}").on("id" -> id).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
}

object Review {
  
  def create(uriId: Int): Boolean = DB.withConnection { implicit conn =>
    try {
      SQL("INSERT INTO reviews (uri_id) VALUES ({uriId})").on("uriId" -> uriId).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def find(id: Int): Option[Review] = DB.withConnection { implicit conn =>
    return Try(mapFromRow(SQL("SELECT * FROM reviews WHERE id={id} LIMIT 1").on("id"->id)().head)).getOrElse(None)
  }
  
  def findByTag(tagId: Int): List[Review] = DB.withConnection { implicit conn =>
    return List()		//TODO WTSN-31
  }
  
  def findByUri(uriId: Int): List[Review] = DB.withConnection { implicit conn =>
    return try {
      SQL("SELECT * FROM reviews WHERE uri_id={uriId} LIMIT 1").on("uriId"->uriId)()
      	.map(mapFromRow).flatten.toList
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      List()
    }
  }
  
  private def mapFromRow(row: SqlRow): Option[Review] = {
    return try {
      Some(Review(
      	row[Int]("id"), 
			  row[Int]("uri_id"),
			  row[Option[Int]]("reviewed_by"),
			  row[Option[Int]]("verified_by"),
			  row[Option[Int]]("review_data_id"),
			  row[Option[Array[Int]]]("review_tag_ids").getOrElse(Array()).toList,
			  row[ReviewStatus]("status"),
			  row[Date]("created_at").getTime / 1000,
			  row[Date]("status_updated_at").getTime / 1000
      ))
    } catch {
      case e: Exception => None
    }
  }
  
}