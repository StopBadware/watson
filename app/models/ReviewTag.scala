package models

import anorm._
import play.api.db._
import play.api.Play.current
import play.api.Logger
import org.postgresql.util.PSQLException
import scala.util.Try

case class ReviewTag(id: Int, name: String, description: Option[String], hexColor: String, active: Boolean) {

  def update(description: Option[String], hexColor: String): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("UPDATE review_tags SET description={description}, hex_color={hexColor} WHERE id={id}")
        .on("id" -> id, "description" -> Try(description.get).getOrElse(null), "hexColor" -> hexColor)
        .executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def toggleActive(active: Boolean): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("UPDATE review_tags SET active={active} WHERE id={id}")
        .on("id" -> id, "active" -> active).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def delete(): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("DELETE FROM review_tags WHERE id={id}").on("id" -> id).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  override def toString: String = name
  
}

object ReviewTag {
  
  def create(name: String, description: Option[String]=None, hexColor: String="000000"): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("""INSERT INTO review_tags (name, description, hex_color, active) SELECT {name}, {description}, 
        {hexColor}, true WHERE NOT EXISTS (SELECT 1 FROM review_tags WHERE name={name})""")
        .on("name"->name.toUpperCase, "description"->Try(description.get).getOrElse(null), "hexColor"->hexColor)
        .executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def find(id: Int): Option[ReviewTag] = DB.withConnection { implicit conn =>
    return Try(mapFromRow(SQL("SELECT * FROM review_tags WHERE id={id} LIMIT 1").on("id"->id)().head)).getOrElse(None)
  }
  
  def findByName(name: String): Option[ReviewTag] = DB.withConnection { implicit conn =>
    return Try(mapFromRow(SQL("SELECT * FROM review_tags WHERE name={name} LIMIT 1")
      .on("name" -> name.toUpperCase)().head)).getOrElse(None)
  }
  
  private def mapFromRow(row: SqlRow): Option[ReviewTag] = {
    return try {  
	    Some(ReviewTag(
		    row[Int]("id"),
		    row[String]("name"),
		    row[Option[String]]("description"),
		    row[String]("hex_color"),
		    row[Boolean]("active")
  		))
    } catch {
      case e: Exception => None
    }
  }
  
}