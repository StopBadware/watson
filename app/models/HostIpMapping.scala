package models

import java.util.Date
import java.sql.Timestamp
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
  
  def create(reversedHost: String, ip: Long, resolvedAt: Long): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("""INSERT INTO host_ip_mappings (reversed_host, ip, resolved_at) SELECT {reversedHost}, {ip}, {resolvedAt} 
        WHERE NOT EXISTS (SELECT 1 FROM (SELECT ip FROM host_ip_mappings WHERE reversed_host={reversedHost} 
        ORDER BY resolved_at DESC LIMIT 1) AS ip WHERE ip={ip} LIMIT 1)""")
        .on("reversedHost" -> reversedHost, "ip" -> ip, "resolvedAt" -> new Timestamp(resolvedAt * 1000))
        .executeUpdate() > 0
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
  
  def top(max: Int): List[TopIp] = DB.withConnection { implicit conn =>
    return try {
      val ipsHosts = SQL("""SELECT ip, COUNT(DISTINCT reversed_host) AS cnt FROM host_ip_mappings WHERE 
        resolved_at=(SELECT resolved_at FROM host_ip_mappings ORDER BY resolved_at DESC LIMIT 1) 
        GROUP BY ip LIMIT {limit}""").on("limit" -> max)()
    		.map(row => (row[Long]("ip"), row[Long]("cnt").toInt)).toMap
      println(ipsHosts)	//DELME WTSN-15
      
      //TopIp(row[Long]("ip"), row[Int]("num"), row[String]("name"), row[Int]("hosts"), row[Int]("uris"))
      List()				//DELME WTSN-15
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      List()
    }
  }
  
  def lastResolvedAt: Long = DB.withConnection { implicit conn =>
    return Try(SQL("SELECT resolved_at FROM host_ip_mappings ORDER BY resolved_at DESC LIMIT 1")()
      .map(_[Date]("").getTime / 1000).head).getOrElse(0)
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

case class TopIp(ip: Long, asNum: Int, asName: String, numHosts: Int, numUris: Int) {}