package models.cr

import java.util.Date
import java.sql.Timestamp
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.Logger
import org.postgresql.util.PSQLException
import scala.util.Try

case class CrType(
    id: Int,
    crType: String,
    createdAt: Long
	) {
  
  def delete(): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("DELETE FROM cr_types WHERE id={id}").on("id" -> id).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def update(newCrType: String): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("UPDATE cr_types SET cr_type={crType} WHERE id={id}").on("crType"->newCrType,"id"->id).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }

}

object CrType {
  
  def create(crType: String): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("INSERT INTO cr_types (cr_type) VALUES ({crType})").on("crType" -> crType).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def find(id: Int): Option[CrType] = DB.withConnection { implicit conn =>
    return Try(mapFromRow(SQL("SELECT * FROM cr_types WHERE id={id} LIMIT 1").on("id"->id)().head)).getOrElse(None)
  }
  
  def findByType(name: String): Option[CrType] = DB.withConnection { implicit conn =>
    return Try(mapFromRow(SQL("SELECT * FROM cr_types WHERE cr_type={name} LIMIT 1").on("name"->name)().head)).getOrElse(None)
  }
  
  def all: List[String] = DB.withConnection { implicit conn =>
    return Try(SQL("SELECT cr_type FROM cr_types ORDER BY cr_type ASC")().map(_[String]("cr_type")).toList).getOrElse(List.empty[String])
  }
  
  private def mapFromRow(row: SqlRow): Option[CrType] = {
    return try {
      Some(CrType(
      	row[Int]("id"), 
			  row[String]("cr_type"),
			  row[Date]("created_at").getTime / 1000
      ))
    } catch {
      case e: Exception => None
    }
  }
  
}