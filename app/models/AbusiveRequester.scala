package models

import scala.util.Try
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.Logger
import org.postgresql.util.PSQLException
import models.enums.Role

case class AbusiveRequester(email: String, flaggedBy: Int, flaggedAt: Long) {
  
}

object AbusiveRequester {
  
  def flag(email: String, userId: Int): Boolean = DB.withConnection { implicit conn =>
    return try {
      if (User.find(userId).get.hasRole(Role.VERIFIER)) {
	      SQL("INSERT INTO abusive_requesters (email, flagged_by) SELECT {email}, {flaggedBy} WHERE NOT EXISTS (SELECT 1 FROM "+
	        "abusive_requesters WHERE email={email})").on("email" -> email.toLowerCase, "flaggedBy" -> userId).executeUpdate() > 0
      } else {
        false
      }
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def unFlag(email: String, userId: Int): Boolean = DB.withConnection { implicit conn =>
    return try {
      if (User.find(userId).get.hasRole(Role.VERIFIER)) {
      	SQL("DELETE FROM abusive_requesters WHERE email={email}").on("email" -> email).executeUpdate() > 0
      } else {
        false
      }
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def isFlagged(email: String): Boolean = DB.withConnection { implicit conn =>
    return Try(SQL("SELECT 1 FROM abusive_requesters WHERE email={email}").on("email" -> email.toLowerCase)().size == 1).getOrElse(false)
  }
  
}