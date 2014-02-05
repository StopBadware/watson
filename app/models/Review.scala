package models

import java.util.Date
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.Logger
import org.postgresql.util.PSQLException
import scala.util.Try
import models.enums._
import controllers.PostgreSql.rowToIntArray

case class Review(
	  id: Int, 
	  uriId: Int,
	  reviewedBy: Option[Int],
	  verifiedBy: Option[Int],
	  reviewDataId: Option[Int],
	  reviewTags: Set[Int],
	  status: ReviewStatus,
	  createdAt: Long,
	  statusUpdatedAt: Long
  ) {
  
  def reviewed(verdict: ReviewStatus, reviewer: Int, reviewData: Option[Int]=None): Boolean = DB.withConnection { implicit conn =>
    val dataId = if (reviewData.isDefined) reviewData else reviewDataId
    val newStatus = if (verdict == ReviewStatus.CLOSED_BAD) ReviewStatus.PENDING_BAD else verdict
    val updated = try {
      if (User.find(reviewer).get.hasRole(Role.REVIEWER)) {
	      SQL("""UPDATE reviews SET status={status}::REVIEW_STATUS, reviewed_by={reviewerId}, 
	        review_data_id={dataId}, status_updated_at=NOW() WHERE id={id}""")
	        .on("id"->id, "status"->newStatus.toString, "reviewerId"->reviewer, "dataId"->dataId).executeUpdate() > 0
      } else {
        false
      }
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
    
    if (updated && verdict == ReviewStatus.CLOSED_CLEAN) {
      updateStatus(verdict)
    }
    
    return updated
  }
  
  def reject(verifier: Int, comments: String): Boolean = DB.withConnection { implicit conn =>
    if (User.find(verifier).get.hasRole(Role.VERIFIER)) {
      //TODO WTSN-18 add comments to review data
	    updateStatus(ReviewStatus.REJECTED, Some(verifier))
	  } else {
	    false
	  }
  }
  
  def verify(verifier: Int, closeAs: ReviewStatus): Boolean = {
    if (User.find(verifier).get.hasRole(Role.VERIFIER)) {
	    updateStatus(closeAs, Some(verifier))
	  } else {
	    false
	  }
	}
  
  def closeNoLongerBlacklisted(): Boolean = updateStatus(ReviewStatus.CLOSED_NO_LONGER_REPORTED)
  
  def closeWithoutReview(): Boolean = updateStatus(ReviewStatus.CLOSED_WITHOUT_REVIEW)
  
  private def updateStatus(newStatus: ReviewStatus, verifier: Option[Int]=None): Boolean = DB.withConnection { implicit conn =>
    val verifierId = if (verifier.isDefined) verifier else verifiedBy
    val updated = try {
      SQL("""UPDATE reviews SET status={status}::REVIEW_STATUS, verified_by={verifierId}, 
        status_updated_at=NOW() WHERE id={id} AND status<='PENDING_BAD'::REVIEW_STATUS""")
        .on("id" -> id, "status" -> newStatus.toString, "verifierId" -> verifierId).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
    
    if (updated && !newStatus.isOpen) {
      val reason = newStatus match {
        case ReviewStatus.CLOSED_BAD => ClosedReason.REVIEWED_BAD
        case ReviewStatus.CLOSED_CLEAN => ClosedReason.REVIEWED_CLEAN
        case ReviewStatus.CLOSED_NO_LONGER_REPORTED => ClosedReason.NO_PARTNERS_REPORTING
        case _ => ClosedReason.ADMINISTRATIVE
      }
      ReviewRequest.findByUri(uriId).filter(_.open).foreach(_.close(reason, Some(id)))
      
      if (newStatus==CLOSED_CLEAN && BlacklistEvent.findBlacklistedByUri(uriId, Some(Source.GOOG)).nonEmpty) {
      	//TODO WTSN-12 add to Google rescan queue       
      }
    }
    
    return updated
  }
  
  def reopen(): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("""UPDATE reviews SET status='REOPENED'::REVIEW_STATUS, status_updated_at=NOW() WHERE id={id}""")
      	.on("id" -> id).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def delete(): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("DELETE FROM reviews WHERE id={id}").on("id" -> id).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def isOpen: Boolean = status.isOpen
  
  def addTag(tagId: Int): Boolean = DB.withConnection { implicit conn =>
    return try {
      if (reviewTags.contains(tagId)) {
        false
      } else {
        SQL("""UPDATE reviews SET review_tag_ids=((SELECT review_tag_ids FROM reviews WHERE id={id}) || ARRAY[{tagId}]) 
          WHERE id={id}""").on("id" -> id, "tagId" -> tagId).executeUpdate() > 0
      }
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def removeTag(tagId: Int): Boolean = DB.withConnection { implicit conn =>
    return try {
      val tags = Try(SQL("SELECT review_tag_ids FROM reviews WHERE id={id}").on("id" -> id)()
        .head[Option[Array[Int]]]("review_tag_ids").getOrElse(Array()).toSet).getOrElse(Set())
      val newTagIds = tags.filter(_!=tagId).mkString(",")
      SQL("UPDATE reviews SET review_tag_ids=ARRAY["+newTagIds+"] WHERE id={id}").on("id"->id).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
}

object Review {
  
  def create(uriId: Int): Boolean = DB.withConnection { implicit conn =>
    //TODO WTSN-12 if blacklisted by Google add to rescan queue
    //TODO WTSN-24 add to scanning queue
    return try {
      SQL("""INSERT INTO reviews (uri_id) SELECT {uriId} WHERE NOT EXISTS 
        (SELECT 1 FROM reviews WHERE uri_id={uriId} AND status<='PENDING_BAD'::REVIEW_STATUS)""")
        .on("uriId" -> uriId).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def find(id: Int): Option[Review] = DB.withConnection { implicit conn =>
    return Try(mapFromRow(SQL("SELECT * FROM reviews WHERE id={id} LIMIT 1").on("id"->id)().head)).getOrElse(None)
  }
  
  def findByTag(tagId: Int): List[Review] = DB.withConnection { implicit conn =>
    return try {
    	SQL("SELECT * FROM reviews WHERE {tagId} = ANY (review_tag_ids)").on("tagId"->tagId)().map(mapFromRow).flatten.toList
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      List()
    }
  }
  
  def findByUri(uriId: Int): List[Review] = DB.withConnection { implicit conn =>
    return try {
      SQL("SELECT * FROM reviews WHERE uri_id={uriId}").on("uriId"->uriId)().map(mapFromRow).flatten.toList
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      List()
    }
  }
  
  def closeAllWithoutOpenReviewRequests(): Int = DB.withConnection { implicit conn =>
    return try {
      SQL("""UPDATE reviews SET status='CLOSED_WITHOUT_REVIEW'::REVIEW_STATUS, status_updated_at=NOW()  
        WHERE reviews.status<='PENDING_BAD'::REVIEW_STATUS AND (SELECT COUNT(*) FROM review_requests  
        WHERE review_requests.open=true AND review_requests.uri_id=reviews.uri_id)=0""").executeUpdate()
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      0
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
			  row[Option[Array[Int]]]("review_tag_ids").getOrElse(Array()).toSet,
			  row[ReviewStatus]("status"),
			  row[Date]("created_at").getTime / 1000,
			  row[Date]("status_updated_at").getTime / 1000
      ))
    } catch {
      case e: Exception => None
    }
  }
  
}