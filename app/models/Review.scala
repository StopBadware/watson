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
import controllers.Redis

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
  
  def closeClean(reviewerId: Int): Boolean = {
    val updated = reviewed(reviewerId, ReviewStatus.CLOSED_CLEAN)
    if (updated) {
	    val uri = Uri.find(uriId)
	    if (uri.isDefined && uri.get.isBlacklistedBy(Source.GOOG)) {
	      Redis.addToGoogleRescanQueue(uri.get.uri)
	    }
    }
    return updated
  }
    
  def markPendingBad(reviewerId: Int): Boolean = reviewed(reviewerId, ReviewStatus.PENDING_BAD) 
  
  private def reviewed(reviewerId: Int, verdict: ReviewStatus): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("""UPDATE reviews SET status={status}::REVIEW_STATUS, reviewed_by={reviewerId}, 
        status_updated_at=NOW() WHERE id={id} AND status<='PENDING_BAD'::REVIEW_STATUS""")
        .on("id" -> id, "status" -> verdict.toString, "reviewerId" -> reviewerId).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def reject(verifierId: Int): Boolean = DB.withConnection { implicit conn =>
    if (User.find(verifierId).get.hasRole(Role.VERIFIER)) {
	    updateStatus(ReviewStatus.REJECTED, Some(verifierId))
	  } else {
	    false
	  }
  }
  
  def verifyBad(verifierId: Int): Boolean = {
    if (User.find(verifierId).get.hasRole(Role.VERIFIER)) {
	    updateStatus(ReviewStatus.CLOSED_BAD, Some(verifierId))
	  } else {
	    false
	  }
	}
  
  def closeNoLongerBlacklisted(): Boolean = updateStatus(ReviewStatus.CLOSED_NO_LONGER_REPORTED)
  
  def closeWithoutReview(): Boolean = updateStatus(ReviewStatus.CLOSED_WITHOUT_REVIEW)
  
  def closeWithoutReview(verifierId: Int): Boolean = {
    val user = User.find(verifierId)
    if (user.isDefined && user.get.hasRole(Role.VERIFIER)) {
    	updateStatus(ReviewStatus.CLOSED_WITHOUT_REVIEW, Some(verifierId))
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
  
  def addTag(tagId: Int): Boolean = addTag(ReviewTag.find(tagId))
  
  def addTag(tagName: String): Boolean = addTag(ReviewTag.findByName(tagName)) 
  
  def addTag(tag: Option[ReviewTag]): Boolean = {
    return if (tag.isDefined) {
      val t = tag.get
      if (t.openOnly) {
        addOpenOnlyTag(t.id)
      } else {
        if (t.isCategory) {
          removeCategories()
        }
        addReviewTag(t.id)
      }
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
        SQL("""UPDATE reviews SET open_only_tag_ids=((SELECT open_only_tag_ids FROM reviews WHERE id={id}) 
          || ARRAY[{tagId}::INTEGER]) WHERE id={id}""").on("id" -> id, "tagId" -> tagId).executeUpdate() > 0
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
  
  private def removeCategories(): Boolean = DB.withConnection { implicit conn =>
    return Try {
      SQL("""DELETE FROM review_taggings USING review_tags tags WHERE review_id={id} AND review_tag_id=tags.id 
        AND tags.is_category=true""")
      	.on("id" -> id).executeUpdate()
    }.isSuccess
  }
  
  private def removeOpenOnlyTag(tagId: Int): Boolean = DB.withConnection { implicit conn =>
    return try {
      val tags = Try(SQL("SELECT open_only_tag_ids FROM reviews WHERE id={id}").on("id" -> id)()
        .head[Option[Array[Int]]]("open_only_tag_ids").getOrElse(Array()).toSet).getOrElse(Set())
      val newTagIds = tags.filterNot(_ == tagId).mkString(",")
      SQL("UPDATE reviews SET open_only_tag_ids=ARRAY["+newTagIds+"]::INTEGER[] WHERE id={id}").on("id"->id).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def addNote(authorId: Int, note: String): Boolean = ReviewNote.create(id, authorId, note)
  
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
    val created = try {
      SQL("""INSERT INTO reviews (uri_id) SELECT {uriId} WHERE NOT EXISTS 
        (SELECT 1 FROM reviews WHERE uri_id={uriId} AND status<='PENDING_BAD'::REVIEW_STATUS)""")
        .on("uriId" -> uriId).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
    
    if (created) {
      //TODO WTSN-24 add to scanning queue
    }
    
    return created
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
  
  def uniqueUrisByStatus: Map[ReviewStatus, Int] = DB.withConnection { implicit conn =>
    return Try {
      SQL("SELECT COUNT(DISTINCT uri_id) AS cnt, status FROM reviews GROUP BY status")().map { row =>
        (row[ReviewStatus]("status"), row[Long]("cnt").toInt)
      }.toMap
    }.getOrElse(Map.empty[ReviewStatus, Int])
  }
  
  private def mapFromRow(row: SqlRow): Option[Review] = {
    return Try {
      Review(
      	row[Int]("id"), 
			  row[Int]("uri_id"),
			  row[Option[Int]]("reviewed_by"),
			  row[Option[Int]]("verified_by"),
			  row[Option[Array[Int]]]("open_only_tag_ids").getOrElse(Array()).toSet,
			  row[ReviewStatus]("status"),
			  row[Date]("created_at").getTime / 1000,
			  row[Date]("status_updated_at").getTime / 1000
      )
    }.toOption
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
	val category = tags.values.filter(_.isCategory).headOption
	val reviewCode = ReviewCode.findByReview(review.id)
	val associatedUris = AssociatedUri.findByReviewId(review.id)
  val uris: Map[Int, String] = {
    val urisToFind = {
      googleRescans.map(rescan => List(rescan.uriId, rescan.relatedUriId.getOrElse(0))).flatten ++ 
      associatedUris.map(_.uriId)
    }.distinct
    Uri.find(urisToFind).map(uri => (uri.id, uri.uri.toString)).toMap
  }
  def tagsWithoutOpenOnly: Map[Int, ReviewTag] = tags.filterNot(_._2.openOnly)
}

case class Note(id: Int, author: String, note: String, createdAt: Long)

object Note {
  
  def mapFromRow(row: SqlRow): Option[Note] = {
    return Try {
      Note(
      	row[Int]("id"), 
			  row[String]("username"),
			  row[String]("note"),
			  row[Date]("created_at").getTime / 1000
      )
    }.toOption
  }
  
}

case class ReviewResult(
		uri: String,
		opened: Long,
		closed: Long,
		status: ReviewStatus,
		category: Option[String],
		badCode: Option[String],
		executableSha256: Option[String]
)

object ReviewResult {
  
  def closedSince(since: Long): List[ReviewResult] = DB.withConnection { implicit conn =>
    return try {
      val rows = SQL("""SELECT reviews.id, uri, reviews.created_at, status_updated_at, status, bad_code, exec_sha2_256 FROM reviews 
        JOIN uris ON reviews.uri_id=uris.id LEFT JOIN review_code ON review_code.review_id=reviews.id WHERE status>='CLOSED_BAD'::REVIEW_STATUS 
        AND status_updated_at>={since}""")
      	.on("since" -> new Timestamp(since * 1000))().toList
      val categories = mapCategories(rows.map(_[Int]("id")))
    	rows.map { row =>
        ReviewResult(
      		row[String]("uri"),
      		row[Date]("created_at").getTime / 1000,
      		row[Date]("status_updated_at").getTime / 1000,
      		row[ReviewStatus]("status"),
      		categories.get(row[Int]("id")),
      		row[Option[String]]("bad_code"),
      		row[Option[String]]("exec_sha2_256")
        )
      }.toList
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      List()
    }
  }
  
  private def mapCategories(reviewIds: List[Int]): Map[Int, String] = DB.withTransaction { implicit conn =>
    return try {
      val sql = "SELECT review_id, name FROM review_taggings JOIN review_tags ON review_tag_id=review_tags.id WHERE review_id IN (?"+
      (",?"*(reviewIds.size-1)) + ") AND is_category=true"
      val ps = conn.prepareStatement(sql)
      reviewIds.foldLeft(1) { (i, id) =>
        ps.setInt(i, id)
        i + 1
      }
      val rs = ps.executeQuery
      Iterator.continually((rs, rs.next())).takeWhile(_._2).map { case (row, hasNext) =>
        (row.getInt("review_id"), row.getString("name"))
      }.toMap
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      Map()
    }
  }
  
}
