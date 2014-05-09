package models

import java.util.Date
import java.sql.Timestamp
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.Logger
import org.postgresql.util.PSQLException
import scala.util.Try

case class HostIpMapping(id: Int, reversedHost: String, ip: Long, firstresolvedAt: Long, lastresolvedAt: Long) {
  
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
  
  private def create(reversedHost: String, ip: Long, resolvedAt: Long): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("""INSERT INTO host_ip_mappings (reversed_host, ip, first_resolved_at, last_resolved_at) 
        SELECT {reversedHost}, {ip}, {resolvedAt}, {resolvedAt} 
        WHERE NOT EXISTS (SELECT 1 FROM (SELECT ip FROM host_ip_mappings WHERE reversed_host={reversedHost} 
        ORDER BY last_resolved_at DESC LIMIT 1) AS ip WHERE ip={ip} LIMIT 1)""")
        .on("reversedHost" -> reversedHost, "ip" -> ip, "resolvedAt" -> new Timestamp(resolvedAt * 1000))
        .executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def createOrUpdate(reversedHost: String, ip: Long, resolvedAt: Long): Boolean = DB.withConnection { implicit conn =>
    return try {
      val found = Try(mapFromRow(SQL("SELECT * FROM host_ip_mappings WHERE reversed_host={reversedHost} ORDER BY last_resolved_at DESC LIMIT 1")
        .on("reversedHost" -> reversedHost)().head).get).toOption
      if (found.isDefined && found.get.ip==ip) {
        SQL("UPDATE host_ip_mappings SET last_resolved_at={resolvedAt} WHERE id={id}")
        	.on("id" -> found.get.id, "resolvedAt" -> new Timestamp(resolvedAt * 1000)).executeUpdate() > 0
      } else {
        create(reversedHost, ip, resolvedAt)
      }
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
    return Try(SQL("SELECT * FROM host_ip_mappings WHERE ip={ip} ORDER BY last_resolved_at DESC")
      .on("ip" -> ip)().map(mapFromRow).flatten.toList).getOrElse(List())
  }
  
  def findByHost(reversedHost: String): List[HostIpMapping] = DB.withConnection { implicit conn =>
    return Try(SQL("SELECT * FROM host_ip_mappings WHERE reversed_host={reversedHost} ORDER BY last_resolved_at DESC")
      .on("reversedHost" -> reversedHost)().map(mapFromRow).flatten.toList).getOrElse(List())
  }
  
  def top(max: Int): List[TopIp] = DB.withConnection { implicit conn =>
    return try {
      val resolvedAt = new Timestamp(lastResolvedAt * 1000)
      val ipsUris = SQL("""SELECT ip, COUNT(*) AS cnt FROM host_ip_mappings JOIN uris ON host_ip_mappings.reversed_host=uris.reversed_host 
        WHERE last_resolved_at={resolvedAt} AND ip>0 GROUP BY ip ORDER BY cnt DESC LIMIT {limit}""")
        .on("resolvedAt" -> resolvedAt, "limit" -> max)()
        .map(row => (row[Long]("ip"), row[Long]("cnt").toInt)).toMap
      val ipsHosts = ipHostCounts(ipsUris.keySet, resolvedAt)
      val ipsAsns = ipAsInfo(ipsUris.keySet, resolvedAt)
      
      ipsUris.map { case (ip, uris) =>
        Try(TopIp(
      		ip,
      		ipsAsns(ip)._1,
      		ipsAsns(ip)._2,
      		ipsHosts(ip),
      		uris
        )).toOption
      }.flatten.toList
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      List()
    }
  }
  
  private def ipHostCounts(ips: Set[Long], resolvedAt: Timestamp): Map[Long, Int] = DB.withTransaction { implicit conn =>
    return try {
      val sql = "SELECT ip, COUNT(DISTINCT reversed_host) AS cnt FROM host_ip_mappings WHERE last_resolved_at=? "+
        "AND ip IN (?" + (",?"*(ips.size-1)) +") GROUP BY ip"
      val ps = conn.prepareStatement(sql)
      ps.setTimestamp(1, resolvedAt)
      ips.foldLeft(2) { (i, ip) =>
        ps.setLong(i, ip)
        i + 1
      }
      val rs = ps.executeQuery
      Iterator.continually((rs, rs.next())).takeWhile(_._2).map { case (row, hasNext) =>
        (row.getLong("ip"), row.getInt("cnt"))
      }.toMap
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      Map()
    }
  }
  
  private def ipAsInfo(ips: Set[Long], mappedAt: Timestamp): Map[Long, (Int, String)] = DB.withTransaction { implicit conn =>
    return try {
      val sql = "SELECT ip, number, name FROM ip_asn_mappings JOIN autonomous_systems ON asn=autonomous_systems.number " + 
        "WHERE last_mapped_at=? AND ip IN (?" + (",?"*(ips.size-1)) + ")"
      val ps = conn.prepareStatement(sql)
      ps.setTimestamp(1, mappedAt)
      ips.foldLeft(2) { (i, ip) =>
        ps.setLong(i, ip)
        i + 1
      }
      val rs = ps.executeQuery
      Iterator.continually((rs, rs.next())).takeWhile(_._2).map { case (row, hasNext) =>
        (row.getLong("ip"), (row.getInt("number"), row.getString("name")))
      }.toMap
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      Map()
    }
  }
  
  def lastResolvedAt: Long = DB.withConnection { implicit conn =>
    return Try(SQL("SELECT last_resolved_at FROM host_ip_mappings ORDER BY last_resolved_at DESC LIMIT 1")()
      .map(_[Date]("last_resolved_at").getTime / 1000).head).getOrElse(0)
  }
  
  private def mapFromRow(row: SqlRow): Option[HostIpMapping] = {
    return Try {
      HostIpMapping(
      	row[Int]("id"),
      	row[String]("reversed_host"),
			  row[Long]("ip"),
			  row[Date]("first_resolved_at").getTime / 1000,
			  row[Date]("last_resolved_at").getTime / 1000
      )
    }.toOption
  }  
  
}

case class TopIp(ip: Long, asNum: Int, asName: String, numHosts: Int, numUris: Int) {}