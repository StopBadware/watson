package models

import anorm._
import play.api.db._
import play.api.Play.current
import play.api.Logger
import org.postgresql.util.PSQLException
import scala.util.Try

case class ReviewTag(
    id: Int, 
    name: String, 
    description: Option[String], 
    hexColor: String,
    openOnly: Boolean,
    isCategory: Boolean,
    active: Boolean) {

  def update(description: Option[String], hexColor: String): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("UPDATE review_tags SET description={description}, hex_color={hexColor} WHERE id={id}")
        .on("id" -> id, "description" -> description, "hexColor" -> hexColor)
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
  override def equals(any: Any): Boolean = name.equals(any.toString) 
  
}

object ReviewTag {
  
  def create(
      name: String, 
      description: Option[String]=None, 
      hexColor: String="000000",
      openOnly: Boolean=false,
      isCategory: Boolean=false
    ): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("""INSERT INTO review_tags (name, description, hex_color, open_only, is_category, active) SELECT {name}, {description}, 
        {hexColor}, {openOnly}, {isCategory}, true WHERE NOT EXISTS (SELECT 1 FROM review_tags WHERE name={name})""")
        .on(
          "name" -> name.toUpperCase, 
          "description" -> description, 
          "hexColor" -> hexColor, 
          "openOnly" -> openOnly,
          "isCategory" -> isCategory)
        .executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def find(id: Int): Option[ReviewTag] = DB.withConnection { implicit conn =>
    return Try(mapFromRow(SQL("SELECT * FROM review_tags WHERE id={id} LIMIT 1").on("id"->id)().head)).getOrElse(None)
  }
  
  def find(ids: List[Int]): List[ReviewTag] = DB.withConnection { implicit conn =>
    return try {
      if (ids.nonEmpty) {
	      val sql = "SELECT * FROM review_tags WHERE id in (?"+(",?"*(ids.size-1))+")"
	      val ps = conn.prepareStatement(sql)
	      for (i <- 1 to ids.size) {
	        ps.setInt(i, ids(i-1))
	      }
	      val rs = ps.executeQuery
	      Iterator.continually((rs, rs.next())).takeWhile(_._2).map { case (row, hasNext) =>
	        Some(ReviewTag(
				    row.getInt("id"),
				    row.getString("name"),
				    Try(Some(row.getString("description"))).getOrElse(None),
				    row.getString("hex_color"),
				    row.getBoolean("open_only"),
				    row.getBoolean("is_category"),
				    row.getBoolean("active")
		  		))
	      }.flatten.toList
      } else {
        List()
      }
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      List()
    }
  }
  
  def allActive: List[ReviewTag] = DB.withConnection { implicit conn =>
    Try(SQL("SELECT * FROM review_tags WHERE active=true")().map(mapFromRow).flatten.toList).getOrElse(List())
  }
  
  def categories: List[ReviewTag] = DB.withConnection { implicit conn =>
    Try(SQL("SELECT * FROM review_tags WHERE is_category=true")().map(mapFromRow).flatten.toList).getOrElse(List())
  }
  
  def findByName(name: String): Option[ReviewTag] = DB.withConnection { implicit conn =>
    return Try(mapFromRow(SQL("SELECT * FROM review_tags WHERE name={name} LIMIT 1")
      .on("name" -> name.toUpperCase)().head)).getOrElse(None)
  }
  
  private def mapFromRow(row: SqlRow): Option[ReviewTag] = {
    return Try {  
	    ReviewTag(
		    row[Int]("id"),
		    row[String]("name"),
		    row[Option[String]]("description"),
		    row[String]("hex_color"),
		    row[Boolean]("open_only"),
		    row[Boolean]("is_category"),
		    row[Boolean]("active")
  		)
    }.toOption
  }
  
}