package models

import java.util.Date
import java.sql.Timestamp
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.Logger
import org.postgresql.util.PSQLException
import scala.util.Try
import controllers.PostgreSql

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
  
  def createOrUpdate(infos: List[AsInfo]): Int = update(infos) + create(infos)
  
  private def create(infos: List[AsInfo]): Int = DB.withTransaction { implicit conn =>
    return try {
      val sql = """INSERT INTO autonomous_systems (number, name, country) SELECT ?, ?, ? WHERE NOT EXISTS 
        (SELECT 1 FROM autonomous_systems WHERE number=?)"""
      val ps = conn.prepareStatement(sql)
      infos.grouped(PostgreSql.batchSize).foldLeft(0) { (total, group) =>
        group.foreach { as =>
          ps.setInt(1, as.number)
          ps.setString(2, as.name)
          ps.setString(3, as.country)
          ps.setInt(4, as.number)
          ps.addBatch()
        }
        val batch = ps.executeBatch()
	  		ps.clearBatch()
	  		total + batch.foldLeft(0)((cnt, b) => cnt + b)
      }
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      0
    }
  }
  
  private def update(infos: List[AsInfo]): Int = DB.withTransaction { implicit conn =>
    val now = new Timestamp(System.currentTimeMillis)
    return try {
      val sql = """UPDATE autonomous_systems SET name=?, country=?, updated_at=? WHERE number=? AND NOT EXISTS 
        (SELECT 1 FROM autonomous_systems WHERE number=? AND name=? AND country=?)"""
      val ps = conn.prepareStatement(sql)
      infos.grouped(PostgreSql.batchSize).foldLeft(0) { (total, group) =>
        group.foreach { as =>
          ps.setString(1, as.name)
          ps.setString(2, as.country)
          ps.setTimestamp(3, now)
          ps.setInt(4, as.number)
          ps.setInt(5, as.number)
          ps.setString(6, as.name)
          ps.setString(7, as.country)
          ps.addBatch()
        }
        val batch = ps.executeBatch()
	  		ps.clearBatch()
	  		total + batch.foldLeft(0)((cnt, b) => cnt + b)
      }
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      0
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

case class AsInfo(number: Int, name: String, country: String)
