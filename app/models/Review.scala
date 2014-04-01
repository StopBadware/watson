package models

import java.util.Date
import java.sql.Timestamp
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.Logger
import org.postgresql.util.PSQLException
import scala.util.Try
import models.enums._
import controllers.PostgreSql._

case class Review(
	  id: Int, 
	  uriId: Int,
	  reviewedBy: Option[Int],
	  verifiedBy: Option[Int],
	  openOnlyTags: Set[Int],
	  status: ReviewStatus,
	  createdAt: Long,
	  statusUpdatedAt: Long
  ) {
  
  lazy val reviewTags = DB.withConnection { implicit conn =>
    try {
	    SQL("SELECT review_tag_id FROM review_taggings WHERE review_id={id}").on("id" -> id)().map(_[Int]("review_tag_id")).toSet
	  } catch {
	    case e: PSQLException => Set.empty[Int]
	  }
  }  
  
  def reviewed(reviewer: Int, verdict: ReviewStatus): Boolean = DB.withConnection { implicit conn =>
    val newStatus = if (verdict == ReviewStatus.CLOSED_BAD) ReviewStatus.PENDING_BAD else verdict
    val updated = try {
      if (User.find(reviewer).get.hasRole(Role.REVIEWER)) {
	      SQL("UPDATE reviews SET status={status}::REVIEW_STATUS, reviewed_by={reviewerId}, status_updated_at=NOW() WHERE id={id}")
	        .on("id"->id, "status"->newStatus.toString, "reviewerId"->reviewer).executeUpdate() > 0
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
  
  def reject(verifier: Int): Boolean = DB.withConnection { implicit conn =>
    if (User.find(verifier).get.hasRole(Role.VERIFIER)) {
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
  
  def closeWithoutReview(verifier: Int): Boolean = {
    val user = User.find(verifier)
    if (user.isDefined && user.get.hasRole(Role.VERIFIER)) {
    	updateStatus(ReviewStatus.CLOSED_WITHOUT_REVIEW, Some(verifier))
    } else {
      false
    }
  }
  
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
      ReviewRequest.findByUri(uriId).filter(_.open).foreach(_.close(reason))
      
      if (newStatus==CLOSED_CLEAN && BlacklistEvent.findBlacklistedByUri(uriId, Some(Source.GOOG)).nonEmpty) {
      	//TODO WTSN-12 add to Google rescan queue       
      }
    }
    
    return updated
  }
  
  def reopen(verifier: Int): Boolean = DB.withConnection { implicit conn =>
    return try {
      val reopened = if (User.find(verifier).get.hasRole(Role.VERIFIER)) {
        SQL("UPDATE reviews SET status='REOPENED'::REVIEW_STATUS, status_updated_at=NOW() WHERE id={id}")
      	.on("id" -> id).executeUpdate() > 0
      } else {
        false
      }
    	if (reopened) {
        ReviewRequest.findByReview(id).filterNot(_.open).foreach(_.reopen())
      }
    	reopened
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
  
  def addTag(tagId: Int): Boolean = {
  		val tag = ReviewTag.find(tagId)
    return if (tag.isDefined) {
      if (tag.get.openOnly) addOpenOnlyTag(tag.get.id) else addReviewTag(tag.get.id) 
    } else {
      false
    }
  }
  
  private def addReviewTag(tagId: Int): Boolean = DB.withConnection { implicit conn =>
    return Try {
      SQL("INSERT INTO review_taggings(review_id, review_tag_id) VALUES({id}, {tagId})")
      	.on("id" -> id, "tagId" -> tagId).executeUpdate()
    }.isSuccess
  }
  
  private def addOpenOnlyTag(tagId: Int): Boolean = DB.withConnection { implicit conn =>
    return try {
      if (openOnlyTags.contains(tagId)) {
        false
      } else {
        SQL("""UPDATE reviews SET open_only_tag_ids=((SELECT open_only_tag_ids FROM reviews WHERE id={id}) || ARRAY[{tagId}]) 
          WHERE id={id}""").on("id" -> id, "tagId" -> tagId).executeUpdate() > 0
      }
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def removeTag(tagId: Int): Boolean = {
    val tag = ReviewTag.find(tagId)
    return if (tag.isDefined) {
      if (tag.get.openOnly) removeOpenOnlyTag(tag.get.id) else removeReviewTag(tag.get.id) 
    } else {
      false
    }
  }
  
  private def removeReviewTag(tagId: Int): Boolean = DB.withConnection { implicit conn =>
    return Try {
      SQL("DELETE FROM review_taggings WHERE review_id={id} AND review_tag_id={tagId}")
      	.on("id" -> id, "tagId" -> tagId).executeUpdate()
    }.isSuccess
  }
  
  private def removeOpenOnlyTag(tagId: Int): Boolean = DB.withConnection { implicit conn =>
    return try {
      val tags = Try(SQL("SELECT open_only_tag_ids FROM reviews WHERE id={id}").on("id" -> id)()
        .head[Option[Array[Int]]]("open_only_tag_ids").getOrElse(Array()).toSet).getOrElse(Set())
      val newTagIds = tags.filterNot(_ == tagId).mkString(",")
      SQL("UPDATE reviews SET open_only_tag_ids=ARRAY["+newTagIds+"] WHERE id={id}").on("id"->id).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def addNote(authorId: Int, note: String): Boolean = ReviewNote.create(id, authorId, note)
  
  def notes: List[Note] = DB.withConnection { implicit conn =>
    return try {
      SQL("""SELECT username, note, review_notes.created_at FROM review_notes JOIN users ON 
        author=users.id WHERE review_id={reviewId}""").on("reviewId" -> id)().map { row =>
        	Note(row[String]("username"), row[String]("note"), row[Date]("created_at").getTime / 1000)
      }.toList
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      List()
    }
  }
  
  def details: ReviewDetails = ReviewDetails(Review.find(this.id).get)
  
  def siblings: Map[String, Option[Int]] = DB.withConnection { implicit conn =>
    return try {
      val op = if (status.isOpen) (if (status.eq(ReviewStatus.PENDING_BAD)) "=" else "<") else ">"
      val sql = "SELECT (SELECT id FROM reviews WHERE created_at < (SELECT created_at FROM reviews WHERE id={id}) "+ 
  	    "AND status"+op+"'PENDING_BAD' ORDER BY created_at DESC LIMIT 1) AS prev, (SELECT id FROM reviews WHERE created_at > "+ 
  	    "(SELECT created_at FROM reviews WHERE id={id}) AND status"+op+"'PENDING_BAD' ORDER BY created_at ASC LIMIT 1) AS next"
	    val row = SQL(sql).on("id" -> id)().head
      Map("prev" -> row[Option[Int]]("prev"), "next" -> row[Option[Int]]("next"))
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      Map()
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
  
  def findOpenOrCreate(uriId: Int): Option[Review] = DB.withConnection { implicit conn =>
    create(uriId)
    return findByUri(uriId).filter(_.isOpen).headOption
  }
  
  def find(id: Int): Option[Review] = DB.withConnection { implicit conn =>
    return Try(mapFromRow(SQL("SELECT * FROM reviews WHERE id={id} LIMIT 1").on("id"->id)().head)).getOrElse(None)
  }
  
  def findByTag(tagId: Int): List[Review] = {
    val tag = ReviewTag.find(tagId)
    return if (tag.isDefined) {
      if (tag.get.openOnly) findByOpenOnlyTag(tag.get.id) else findByReviewTag(tag.get.id) 
    } else {
      List.empty[Review]
    }
  }
  
  private def findByReviewTag(tagId: Int): List[Review] = DB.withConnection { implicit conn =>
    return try {
    	SQL("""SELECT reviews.* FROM review_taggings JOIN reviews ON review_taggings.review_id=reviews.id 
  	    WHERE review_taggings.review_tag_id={tagId}""").on("tagId"->tagId)().map(mapFromRow).flatten.toList
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      List()
    }
  }
  
  private def findByOpenOnlyTag(tagId: Int): List[Review] = DB.withConnection { implicit conn =>
    return try {
    	SQL("SELECT * FROM reviews WHERE {tagId} = ANY (open_only_tag_ids)")
    		.on("tagId" -> tagId)().map(mapFromRow).flatten.toList
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      List()
    }
  }  
  
  def findByUri(uriId: Int): List[Review] = DB.withConnection { implicit conn =>
    return try {
      SQL("SELECT * FROM reviews WHERE uri_id={uriId} ORDER BY created_at DESC")
      	.on("uriId"->uriId)().map(mapFromRow).flatten.toList
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
  
  def summaries(params: ReviewSummaryParams, limit: Int=5000): List[ReviewSummary] = DB.withConnection { implicit conn =>
    return try {
      val times = params.createdAt
      val rows = SQL("SELECT uris.id AS uri_id, reviews.id AS review_id, uri, reviews.status, " +
        "ARRAY(SELECT DISTINCT(email) FROM review_requests WHERE review_requests.review_id=reviews.id "+
        "AND open={open}) AS emails, " +
        "reviews.created_at, reviews.open_only_tag_ids FROM reviews LEFT JOIN uris ON reviews.uri_id=uris.id " + 
        "WHERE status"+params.operator+"{status}::REVIEW_STATUS AND reviews.created_at BETWEEN {start} AND {end} " +
        "ORDER BY reviews.created_at ASC LIMIT {limit}").on(
          "open" -> params.reviewStatus.isOpen, 
          "status" -> params.reviewStatus.toString,
          "start" -> times._1,
          "end" -> times._2,
          "limit" -> limit
    		)()
      val summaries = rows.map { row =>
      	ReviewSummary(
    	    row[Int]("review_id"),
    			row[String]("uri"),
    			row[ReviewStatus]("status"),
    			BlacklistEvent.findBlacklistedByUri(row[Int]("uri_id")).map(_.source).toList.sortBy(_.abbr),
    			row[Array[String]]("emails").toList.sorted,
    			row[Date]("created_at").getTime / 1000,
    			ReviewTag.find(row[Option[Array[Int]]]("open_only_tag_ids").getOrElse(Array()).toList)
      	)
      }.toList
      if (params.blacklistedBy.isDefined) summaries.filter(_.blacklistedBy.contains(params.blacklistedBy.get)) else summaries
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
			  row[Option[Array[Int]]]("open_only_tag_ids").getOrElse(Array()).toSet,
			  row[ReviewStatus]("status"),
			  row[Date]("created_at").getTime / 1000,
			  row[Date]("status_updated_at").getTime / 1000
      ))
    } catch {
      case e: Exception => None
    }
  }
  
}

case class ReviewSummary(
  reviewId: Int,
	uri: String,
	status: ReviewStatus,
	blacklistedBy: List[Source],
	requestEmails: List[String],
	createdAt: Long,
	openOnlyTags: List[ReviewTag]
)

class ReviewSummaryParams(status: Option[String], blacklisted: Option[String], created: Option[String]) {
  
  private val parsedStatus = Try(ReviewStatus.fromStr(status.get.replaceAll("[\\- ]", "_")).get)
  val reviewStatus = parsedStatus.getOrElse(ReviewStatus.PENDING_BAD)
  val operator = if (parsedStatus.isSuccess) "=" else (if (status.equals(Some("all-closed"))) ">" else "<=")
  val blacklistedBy = Source.withAbbr(blacklisted.getOrElse(""))
  val createdAt = parseTimes(created.getOrElse(""))
}

case class ReviewDetails(review: Review) {
  
  val uri = Uri.find(review.uriId).get
	val otherReviews = Review.findByUri(uri.id).filterNot(_.id==review.id)
	val blacklistEvents = BlacklistEvent.findByUri(uri.id)
	val googleRescans = GoogleRescan.findByUri(uri.id)
	val reviewRequests = ReviewRequest.findByUri(uri.id)
	val tags = ReviewTag.find((review+:otherReviews).map(_.reviewTags).flatten.distinct).map(t => (t.id, t)).toMap
	
  def tagsWithoutOpenOnly: Map[Int, ReviewTag] = tags.filterNot(_._2.openOnly)
  
  def rescanUris: Map[Int, String] = {
    //TODO WTSN-55 get all related uris
    Uri.find(googleRescans.map(rescan => List(rescan.uriId, rescan.relatedUriId.getOrElse(0))).flatten.toSet.toList)
      .map(uri => (uri.id, uri.uri.toString)).toMap
  }
  
}

case class Note(author: String, note: String, createdAt: Long)
