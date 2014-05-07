package models

import java.util.Date
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.Logger
import org.postgresql.util.PSQLException
import scala.util.Try

case class HostIpMapping(id: Int, reversedHost: String, ip: Long, resolvedAt: Long) {
  
  def delete(): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("DELETE FROM host_ip_mappings WHERE id={id}").on("id" -> id).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  } 

}

object HostIpMapping {
  
  def create(reversedHost: String, ip: Long): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("""INSERT INTO host_ip_mappings (reversed_host, ip) SELECT {reversedHost}, {ip} WHERE NOT EXISTS (SELECT 1 FROM 
        (SELECT ip FROM host_ip_mappings WHERE reversed_host={reversedHost} ORDER BY resolved_at DESC LIMIT 1) AS ip WHERE ip={ip} LIMIT 1)""")
        .on("reversedHost" -> reversedHost, "ip" -> ip).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def find(id: Int): Option[HostIpMapping] = DB.withConnection { implicit conn =>
    return Try(mapFromRow(SQL("SELECT * FROM host_ip_mappings WHERE id={id} LIMIT 1")
      .on("id" -> id)().head)).getOrElse(None)
  }
  
  def findByIp(ip: Long): List[HostIpMapping] = DB.withConnection { implicit conn =>
    return Try(SQL("SELECT * FROM host_ip_mappings WHERE ip={ip} ORDER BY resolved_at DESC")
      .on("ip" -> ip)().map(mapFromRow).flatten.toList).getOrElse(List())
  }
  
  def findByHost(reversedHost: String): List[HostIpMapping] = DB.withConnection { implicit conn =>
    return Try(SQL("SELECT * FROM host_ip_mappings WHERE reversed_host={reversedHost} ORDER BY resolved_at DESC")
      .on("reversedHost" -> reversedHost)().map(mapFromRow).flatten.toList).getOrElse(List())
  }
  
  private def mapFromRow(row: SqlRow): Option[HostIpMapping] = {
    return Try {
      HostIpMapping(
      	row[Int]("id"),
      	row[String]("reversed_host"),
			  row[Long]("ip"),
			  row[Date]("resolved_at").getTime / 1000
      )
    }.toOption
  }  
  
}