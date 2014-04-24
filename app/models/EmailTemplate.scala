package models

import java.util.Date
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.Logger
import org.postgresql.util.PSQLException
import scala.util.Try

case class EmailTemplate(name: String, subject: String, body: String, modifiedBy: Option[Int], modifiedAt: Long) {
  
  def update(newSubject: String, newBody: String, modifiedById: Option[Int]=None): Boolean = DB.withConnection { implicit conn =>
    return try {
    	SQL("UPDATE email_templates SET subject={subject}, body={body}, modified_by={modifiedById}, modified_at=NOW() WHERE name={name}")
	      .on("name" -> name, "subject" -> newSubject, "body" -> newBody, "modifiedById" -> modifiedById)
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
    return Try(mapFromRow(SQL("SELECT * FROM email_templates WHERE name={name} LIMIT 1").on("name"->name)().head)).getOrElse(None)
  }
  
  def all: List[EmailTemplate] = DB.withConnection { implicit conn =>
    return Try(SQL("SELECT * FROM email_templates ORDER BY name ASC")().map(mapFromRow).flatten.toList).getOrElse(List())
  }
  
  private def mapFromRow(row: SqlRow): Option[EmailTemplate] = {
    return try {
      Some(EmailTemplate(
      	row[String]("name"), 
			  row[String]("subject"),
			  row[String]("body"),
			  row[Option[Int]]("modified_by"),
			  row[Date]("modified_at").getTime / 1000
      ))
    } catch {
      case e: Exception => None
    }
  }
  
}