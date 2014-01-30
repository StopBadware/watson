package models

import anorm._
import play.api.db._
import play.api.Play.current
import play.api.Logger
import org.postgresql.util.PSQLException

case class EmailTemplate(name: String, subject: String, body: String, userId: Option[Int]) {
  
  def update(newSubject: String, newBody: String, modifiedBy: Option[Int]=None): Boolean = DB.withConnection { implicit conn =>
    return try {
    	SQL("UPDATE email_templates SET subject={subject}, body={body}, modified_by={modifiedBy} WHERE name={name}")
      .on("name" -> name, "subject" -> newSubject, "body" -> newBody, "modifiedBy" -> modifiedBy)
      .executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def delete(): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("DELETE FROM email_templates WHERE name={name}").on("name" -> name).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }

}

object EmailTemplate {
  
  def create(name: String, subject: String, body: String, userId: Option[Int]=None): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("""INSERT INTO email_templates (name, subject, body, modified_by) SELECT {name}, {subject}, {body}, 
      {modifiedBy} WHERE NOT EXISTS (SELECT 1 FROM email_templates WHERE name={name})""")
      .on("name" -> name, "subject" -> subject, "body" -> body, "modifiedBy" -> userId).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def find(name: String): Option[EmailTemplate] = DB.withConnection { implicit conn =>
    return try {
      SQL("SELECT * FROM email_templates WHERE name={name} LIMIT 1").on("name"->name)().map { row =>
	      val subject = row[String]("subject")
	      val body = row[String]("body")
	      val userId = row[Option[Int]]("modified_by")
	      EmailTemplate(name, subject, body, userId)
	    }.headOption
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      None
    }
  }
  
}