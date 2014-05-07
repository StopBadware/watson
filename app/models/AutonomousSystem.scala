package models

import java.util.Date
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.Logger
import org.postgresql.util.PSQLException
import scala.util.Try

case class AutonomousSystem(number: Int, name: String, country: String, updatedAt: Long) {
  
  def update(newName: String, newCountry: String): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("UPDATE autonomous_systems SET name={newName}, country={newCountry}, updated_at=NOW() WHERE number={number}")
      	.on("number" -> number, "newCountry" -> newCountry, "newName" -> newName).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def delete(): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("DELETE FROM autonomous_systems WHERE number={number}").on("number" -> number).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  } 

}

object AutonomousSystem {
  
  private def create(number: Int, name: String, country: String): Boolean = DB.withConnection { implicit conn =>
    println(number, name, country, country.length)	//DELME WTSN-14
    return try {
      SQL("""INSERT INTO autonomous_systems (number, name, country) SELECT {number}, {name}, {country}  
    		WHERE NOT EXISTS (SELECT 1 FROM autonomous_systems WHERE number={number})""")
        .on("number" -> number, "name" -> name, "country" -> country.toUpperCase).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def createOrUpdate(number: Int, name: String, country: String): Boolean = {
    val existing = find(number)
    return if (existing.isDefined) {
      val as = existing.get
      if (!as.name.equalsIgnoreCase(name) || !as.country.equalsIgnoreCase(country)) {
      	existing.get.update(name, country)
      } else {
        true
      }
    } else {
      create(number, name, country)
    }
  }
  
  def find(number: Int): Option[AutonomousSystem] = DB.withConnection { implicit conn =>
    return Try(mapFromRow(SQL("SELECT * FROM autonomous_systems WHERE number={number} LIMIT 1").on("number"->number)().head)).getOrElse(None)
  }
  
  private def mapFromRow(row: SqlRow): Option[AutonomousSystem] = {
    return Try {
      AutonomousSystem(
      	row[Int]("number"), 
			  row[String]("name"),
			  row[String]("country"),
			  row[Date]("updated_at").getTime / 1000
      )
    }.toOption
  }
  
}

case class Asn(asn: Int, name: String, country: String)