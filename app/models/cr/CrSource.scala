package models.cr

import java.util.Date
import java.sql.Timestamp
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.Logger
import org.postgresql.util.PSQLException
import scala.util.Try

case class CrSource(
		id: Int,
		shortName: String,
		fullName: String,
		createdAt: Long
	) {
  
  def delete(): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("DELETE FROM cr_sources WHERE id={id}").on("id" -> id).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def update(newShortName: String, newFullName: String): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("UPDATE cr_sources SET short_name={shortName}, full_name={fullName} WHERE id={id}")
      	.on("shortName"->newShortName, "fullName"->newFullName, "id"->id).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
}

object CrSource {
  
  def create(shortName: String, fullName: String): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("INSERT INTO cr_sources (short_name, full_name) VALUES ({shortName}, {fullName})").on(
      		"shortName" -> shortName,
      		"fullName" -> fullName
        ).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def find(id: Int): Option[CrSource] = DB.withConnection { implicit conn =>
    return Try(mapFromRow(SQL("SELECT * FROM cr_sources WHERE id={id} LIMIT 1").on("id"->id)().head)).getOrElse(None)
  }
  
  def findByName(shortName: String): Option[CrSource] = DB.withConnection { implicit conn =>
    return Try(mapFromRow(SQL("SELECT * FROM cr_sources WHERE short_name={shortName} LIMIT 1")
      .on("shortName" -> shortName)().head)).getOrElse(None)
  }
  
  private def mapFromRow(row: SqlRow): Option[CrSource] = {
    return try {
      Some(CrSource(
      	row[Int]("id"), 
			  row[String]("short_name"),
			  row[String]("full_name"),
			  row[Date]("created_at").getTime / 1000
      ))
    } catch {
      case e: Exception => None
    }
  }
  
}