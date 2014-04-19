package models

import java.util.Date
import java.sql.Timestamp
import scala.util.Try
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.Logger
import org.postgresql.util.PSQLException
import controllers.{Email, Mailer}
import models.enums._

case class ReviewRequest(
    id: Int,
    uriId: Int,
    open: Boolean,
    email: String,
    ip: Option[Long],
    requesterNotes: Option[String],
    requestedAt: Long,
    closedAt: Option[Long],
    closedReason: Option[ClosedReason],
    reviewId: Int
    ) {
  
  def delete(): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("DELETE FROM review_requests WHERE id={id}").on("id" -> id).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def reopen(): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("UPDATE review_requests SET open=TRUE, closed_at=NULL, closed_reason=NULL WHERE id={id}")
      	.on("id"->id).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def responses: Map[String, String] = DB.withConnection { implicit conn =>
    return try {
    	SQL("""SELECT question, answer FROM request_responses AS rr JOIN request_questions ON rr.question_id=request_questions.id 
  	    JOIN request_answers on rr.answer_id=request_answers.id WHERE review_request_id={id} ORDER BY question ASC""")
  	    .on("id" -> id)().map(row => (row[String]("question"), row[String]("answer"))).toMap
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      Map()
    }
  }
  
  def close(reason: ClosedReason, closedAt: Option[Long]=None): Boolean = DB.withConnection { implicit conn =>
    val updatedReason = if (closedReason.equals(Some(REVIEWED_CLEAN)) && reason.equals(NO_PARTNERS_REPORTING)) {
      REVIEWED_CLEAN.toString
    } else {
      reason.toString
    }
    val closeTime = closedAt.getOrElse(System.currentTimeMillis / 1000)
    val closed = try {
      SQL("UPDATE review_requests SET open={open}, closed_at={closedAt}, closed_reason={reason}::CLOSED_REASON WHERE id={id}")
      	.on("id" -> id,
    	    "reason" -> updatedReason,
    	    "open" -> reason.equals(REVIEWED_CLEAN),
    	    "closedAt" -> new Timestamp(closeTime * 1000))
      	.executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
    
    if (closed) {
      sendNotification(reason, reviewId)
      if (ReviewRequest.findByReview(reviewId).filter(_.open).isEmpty) {
        if (reason == ClosedReason.NO_PARTNERS_REPORTING) {
          Review.find(reviewId).get.closeNoLongerBlacklisted()
        } else {
          Review.find(reviewId).get.closeWithoutReview()
        }
      }
    }
    closed
  }
  
  private def sendNotification(reason: ClosedReason, reviewId: Int) = {
    val uri = Try(Uri.find(uriId).get.uri).getOrElse("")
    reason match {
      case NO_PARTNERS_REPORTING => Mailer.sendNoLongerBlacklisted(email, uri)
      case REVIEWED_BAD => {
        val notes = ""	//TODO WTSN-18 retrieve notes from review
        Mailer.sendReviewClosedBad(email, uri, notes)
      }
      case REVIEWED_CLEAN => {
        if (BlacklistEvent.findBlacklistedByUri(uriId).filter(_.source==Source.TTS).nonEmpty) {
          Mailer.sendReviewClosedCleanTts(email, uri)
        }
      }
      case _ => Logger.info("Sending no notification for review request "+id+" closed as "+reason)
    }
  }
  
}

object ReviewRequest {
  
  def create(uriId: Int,
    email: String,
    ip: Option[Long]=None,
    notes: Option[String]=None): Boolean = DB.withConnection { implicit conn =>
    val review = Review.findOpenOrCreate(uriId)
    val created = try {
      if (Email.isValid(email) && review.isDefined) {
	      SQL("""INSERT INTO review_requests (uri_id, email, ip, requester_notes, review_id) 
	        VALUES({uriId}, {email}, {ip}, {notes}, {reviewId})""")
	        .on("uriId" -> uriId, "email" -> email, "ip" -> ip, "notes" -> notes, "reviewId" -> review.get.id)
	        .executeUpdate() > 0
      } else {
        false
      }
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
    
    if (created) {
      val uri = Try(Uri.find(uriId).get.uri).getOrElse("")
      Mailer.sendReviewRequestReceived(email, uri)
    }
    created
  }
  
  def createAndFind(uriId: Int,
    email: String,
    ip: Option[Long]=None,
    notes: Option[String]=None): Option[ReviewRequest] = DB.withConnection { implicit conn =>
      val created = create(uriId, email, ip, notes)
      if (created) {
      	Try(mapFromRow(SQL("""SELECT * FROM review_requests WHERE uri_id={uriId} AND email={email} AND open=true 
    	    ORDER BY requested_at DESC LIMIT 1""").on("uriId" -> uriId, "email" -> email)().head)).getOrElse(None)
      } else {
        None
      }
    }
  
  def find(id: Int): Option[ReviewRequest] = DB.withConnection { implicit conn =>
    return Try(mapFromRow(SQL("SELECT * FROM review_requests WHERE id={id}").on("id" -> id)().head)).getOrElse(None)
  }
  
  def findByUri(uriId: Int): List[ReviewRequest] = DB.withConnection { implicit conn =>
    return try {
      SQL("SELECT * FROM review_requests WHERE uri_id={uriId} ORDER BY requested_at DESC")
      	.on("uriId" -> uriId)().map(mapFromRow).toList.flatten
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      List()
    }
  }
  
  def findByReview(reviewId: Int): List[ReviewRequest] = DB.withConnection { implicit conn =>
    return try {
      SQL("SELECT * FROM review_requests WHERE review_id={id}").on("id" -> reviewId)().map(mapFromRow).toList.flatten
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      List()
    }
  }
  
  def findByClosedReason(reason: Option[ClosedReason], times: (Timestamp, Timestamp), limit: Int=5000): List[ReviewRequest] = DB.withConnection { implicit conn =>
    return try {
      val sql = if (reason.isDefined) {
        SQL("SELECT * FROM review_requests WHERE closed_reason={closedReason}::CLOSED_REASON AND requested_at BETWEEN {start} AND {end} "+
        "ORDER BY requested_at DESC LIMIT {limit}")
      	.on("closedReason" -> reason.get.toString, 
      	    "start" -> times._1, 
      	    "end" -> times._2,
      	    "limit" -> limit)
      } else {
        SQL("SELECT * FROM review_requests WHERE open=false AND requested_at BETWEEN {start} AND {end} "+
        "ORDER BY requested_at DESC LIMIT {limit}")
      	.on("start" -> times._1, 
      	    "end" -> times._2,
      	    "limit" -> limit)
      }
      sql().map(mapFromRow).toList.flatten
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      List()
    }
  }  
  
  def findOpen(times: Option[(Timestamp, Timestamp)]=None): List[ReviewRequest] = DB.withConnection { implicit conn =>
    return try {
      val sql = if (times.isDefined) {
        SQL("SELECT * FROM review_requests WHERE open=true AND requested_at BETWEEN {start} AND {end} "+
          "ORDER BY requested_at DESC").on("start" -> times.get._1, "end" -> times.get._2)
      } else {
        SQL("SELECT * FROM review_requests WHERE open=true ORDER BY requested_at DESC")
      }
      sql().map(mapFromRow).toList.flatten
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      List()
    }
  }
  
  def closeNoLongerBlacklisted(): Int = DB.withConnection { implicit conn =>
    case class ToEmail(email: String, uri: String)
    return try {
    	val sql = SQL("""SELECT rr.uri_id, rr.email, uris.uri FROM review_requests AS rr LEFT JOIN 
  	    (SELECT review_requests.uri_id, count(*) AS cnt FROM review_requests LEFT JOIN blacklist_events ON 
    	  review_requests.uri_id=blacklist_events.uri_id WHERE review_requests.open=true AND 
    	  blacklist_events.blacklisted=true GROUP BY review_requests.uri_id) AS current ON rr.uri_id=current.uri_id 
    	  JOIN uris ON rr.uri_id=uris.id WHERE rr.open=true AND current.uri_id IS NULL""")()
    	val urisEmails = sql.foldLeft((Set.empty[Int], Set.empty[ToEmail])) { (sets, row) =>
    	  val uriId = row[Int]("uri_id")
    	  val email = row[String]("email")
    	  val uri = row[String]("uri")
    	  (sets._1 + uriId, sets._2 + ToEmail(email, uri))
    	}
    	val closed = closeAsNotBlacklisted(urisEmails._1)
    	urisEmails._2.foreach { emailUri =>
    	  Mailer.sendNoLongerBlacklisted(emailUri.email, emailUri.uri)
    	}
    	Review.closeAllWithoutOpenReviewRequests()
    	closed
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      0
    }
  }
  
  private def closeAsNotBlacklisted(uriIds: Iterable[Int]): Int = DB.withTransaction { implicit conn =>
    return try {
      val sql = "UPDATE review_requests SET open=false, closed_at=NOW(), closed_reason='"+
    		NO_PARTNERS_REPORTING.toString+"'::CLOSED_REASON WHERE uri_id=? AND open=true"
      val ps = conn.prepareStatement(sql)
      uriIds.foreach { id =>
        ps.setInt(1, id)
        ps.addBatch()
      }
      ps.executeBatch().foldLeft(0)((cnt, b) => cnt + b)
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      0
    }
  }
  
  private def mapFromRow(row: SqlRow): Option[ReviewRequest] = {
    val closedAt = if (row[Option[Date]]("closed_at").isDefined) {
      Some(row[Option[Date]]("closed_at").get.getTime / 1000)
    } else {
      None
    }
    return try {  
	    Some(ReviewRequest(
		    row[Int]("id"),
		    row[Int]("uri_id"),
		    row[Boolean]("open"),
		    row[String]("email"),
		    row[Option[Long]]("ip"),
		    row[Option[String]]("requester_notes"),
		    row[Date]("requested_at").getTime / 1000,
		    closedAt,
		    row[Option[ClosedReason]]("closed_reason"),
		    row[Int]("review_id")
  		))
    } catch {
      case e: Exception => None
    }
  }   
  
}
