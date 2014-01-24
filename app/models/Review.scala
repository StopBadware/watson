package models

import anorm._
import play.api.db._
import play.api.Play.current
import play.api.Logger
import org.postgresql.util.PSQLException
import scala.util.Try
import models.enums.ReviewStatus

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
    return false	//TODO WTSN-31
  }
  
  def find(id: Int): Option[Review] = DB.withConnection { implicit conn =>
    return None		//TODO WTSN-31
  }
  
  def findByTag(tagId: Int): List[Review] = DB.withConnection { implicit conn =>
    return List()		//TODO WTSN-31
  }
  
  def findByUri(uriId: Int): List[Review] = DB.withConnection { implicit conn =>
    return List()		//TODO WTSN-31
  }
  
}