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
    return false 		//TODO WTSN-31
  }
  
  def reject(verifier: Int, comments: String): Boolean = DB.withConnection { implicit conn =>
    //TODO WTSN-31 add comments to review data
    return false 		//TODO WTSN-31
  }
  
  def close(closeAs: ReviewStatus, verifier: Option[Int]=None): Boolean = DB.withConnection { implicit conn =>
    return false 		//TODO WTSN-31
  }
  
//  def updateStatus(description: Option[String], hexColor: String): Boolean = DB.withConnection { implicit conn =>
//    return try {
//      SQL("UPDATE reviews SET description={description}, hex_color={hexColor} WHERE id={id}")
//        .on("id" -> id, "description" -> Try(description.get).getOrElse(null), "hexColor" -> hexColor)
//        .executeUpdate() > 0
//    } catch {
//      case e: PSQLException => Logger.error(e.getMessage)
//      false
//    }
//  }	//DELME WTSN-31
  
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
//      SQL("SELECT * FROM reviews WHERE uri_id={uriId} LIMIT 1").on("uriId"->uriId)()
      SQL("SELECT * FROM reviews WHERE uri_id={uriId} LIMIT 1").on("uriId"->1)()	//TODO REVERT WTSN-31
      	.map(mapFromRow).flatten.toList
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      List()
    }
  }
  
  private def mapFromRow(row: SqlRow): Option[Review] = {
    val tags  = {
      val array = row[Option[Array[Int]]]("review_tag_ids").getOrElse(Array())
//      (tag <- array)
      List()
    }
    println(tags)	//DELME WTSN-31
    return try {
      Some(Review(
      	row[Int]("id"), 
			  row[Int]("uri_id"),
			  row[Option[Int]]("reviewed_by"),
			  row[Option[Int]]("verified_by"),
			  row[Option[Int]]("review_data_id"),
			  tags,
			  row[ReviewStatus]("status"),
			  row[Date]("created_at").getTime / 1000,
			  row[Date]("status_updated_at").getTime / 1000
      ))
    } catch {
      case e: Exception => None
    }
  }
  
}