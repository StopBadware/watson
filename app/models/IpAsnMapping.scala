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

case class IpAsnMapping(id: Int, ip: Long, asn: Int, firstMappedAt: Long, lastMappedAt: Long) {

  def delete(): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("DELETE FROM ip_asn_mappings WHERE id={id}").on("id" -> id).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  } 
  
}

object IpAsnMapping {
  
  def createOrUpdate(mappings: Map[Long, Int], mappedAt: Long): Int = update(mappings, mappedAt) + create(mappings, mappedAt)
  
  private def create(mappings: Map[Long, Int], mappedAt: Long): Int = DB.withTransaction { implicit conn =>
    val time = new Timestamp(mappedAt * 1000)
    return try {
      val sql = """INSERT INTO ip_asn_mappings (ip, asn, first_mapped_at, last_mapped_at) SELECT ?, ?, ?, ? WHERE NOT EXISTS 
        (SELECT 1 FROM (SELECT asn FROM ip_asn_mappings WHERE ip=? ORDER BY last_mapped_at DESC LIMIT 1) AS asn 
        WHERE asn=? LIMIT 1)"""
      val ps = conn.prepareStatement(sql)
      mappings.grouped(PostgreSql.batchSize).foldLeft(0) { (total, group) =>
        group.foreach { case (ip, asn) =>
          ps.setLong(1, ip)
          ps.setInt(2, asn)
          ps.setTimestamp(3, time)
          ps.setTimestamp(4, time)
          ps.setLong(5, ip)
          ps.setInt(6, asn)
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
  
  private def update(mappings: Map[Long, Int], mappedAt: Long): Int = DB.withTransaction { implicit conn =>
    val time = new Timestamp(mappedAt * 1000)
    return try {
      val sql = """UPDATE ip_asn_mappings SET last_mapped_at=? WHERE ip=? AND asn=? AND EXISTS (SELECT 1 FROM 
        (SELECT asn FROM ip_asn_mappings WHERE ip=? ORDER BY last_mapped_at DESC LIMIT 1) AS asn WHERE asn=?)"""
      val ps = conn.prepareStatement(sql)
      mappings.grouped(PostgreSql.batchSize).foldLeft(0) { (total, group) =>
        group.foreach { case (ip, asn) =>
          ps.setTimestamp(1, time)
          ps.setLong(2, ip)
          ps.setInt(3, asn)
          ps.setLong(4, ip)
          ps.setInt(5, asn)
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
  
  def find(id: Int): Option[IpAsnMapping] = DB.withConnection { implicit conn =>
    return Try(mapFromRow(SQL("SELECT * FROM ip_asn_mappings WHERE id={id} LIMIT 1")
      .on("id" -> id)().head)).getOrElse(None)
  }
  
  def findByIp(ip: Long): List[IpAsnMapping] = DB.withConnection { implicit conn =>
    return Try(SQL("SELECT * FROM ip_asn_mappings WHERE ip={ip} ORDER BY last_mapped_at DESC")
      .on("ip" -> ip)().map(mapFromRow).flatten.toList).getOrElse(List())
  }
  
  def findByAsn(asn: Int): List[IpAsnMapping] = DB.withConnection { implicit conn =>
    return Try(SQL("SELECT * FROM ip_asn_mappings WHERE asn={asn} ORDER BY last_mapped_at DESC")
      .on("asn" -> asn)().map(mapFromRow).flatten.toList).getOrElse(List())
  }
  
  def top(max: Int): List[TopAs] = DB.withConnection { implicit conn =>
    return try {
      val mappedAt = new Timestamp(lastMappedAt * 1000)
      val asnsUris = SQL("""SELECT asn, COUNT(*) AS cnt FROM ip_asn_mappings JOIN host_ip_mappings ON ip_asn_mappings.ip=host_ip_mappings.ip 
        JOIN uris ON host_ip_mappings.reversed_host=uris.reversed_host WHERE last_resolved_at={mappedAt} AND 
        last_mapped_at={mappedAt} AND ip_asn_mappings.ip>0 GROUP BY asn ORDER BY cnt DESC LIMIT {limit}""")
        .on("mappedAt" -> mappedAt, "limit" -> max)()
        .map(row => (row[Int]("asn"), row[Long]("cnt").toInt)).toMap
      val asnsIps = asIpCounts(asnsUris.keySet, mappedAt)
      val names = asNames(asnsUris.keySet)
      
      asnsUris.map { case (asn, uris) =>
        Try(TopAs(
      		asn,
      		names(asn),
      		asnsIps(asn),
      		uris
        )).toOption
      }.flatten.toList
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      List()
    }
  }
  
  private def asIpCounts(asns: Set[Int], mappedAt: Timestamp): Map[Int, Int] = DB.withTransaction { implicit conn =>
    return try {
      val sql = "SELECT asn, COUNT(DISTINCT ip) AS cnt FROM ip_asn_mappings WHERE last_mapped_at=? "+
        "AND asn IN (?" + (",?"*(asns.size-1)) +") GROUP BY asn"
      val ps = conn.prepareStatement(sql)
      ps.setTimestamp(1, mappedAt)
      asns.foldLeft(2) { (i, asn) =>
        ps.setInt(i, asn)
        i + 1
      }
      val rs = ps.executeQuery
      Iterator.continually((rs, rs.next())).takeWhile(_._2).map { case (row, hasNext) =>
        (row.getInt("asn"), row.getInt("cnt"))
      }.toMap
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      Map()
    }
  }
  
  private def asNames(asns: Set[Int]): Map[Int, String] = DB.withTransaction { implicit conn =>
    return try {
      val sql = "SELECT number, name FROM autonomous_systems WHERE number IN  (?" + (",?"*(asns.size-1)) +")"
      val ps = conn.prepareStatement(sql)
      asns.foldLeft(1) { (i, asn) =>
        ps.setInt(i, asn)
        i + 1
      }
      val rs = ps.executeQuery
      Iterator.continually((rs, rs.next())).takeWhile(_._2).map { case (row, hasNext) =>
        (row.getInt("number"), row.getString("name"))
      }.toMap
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      Map()
    }
  }
  
  def lastMappedAt: Long = DB.withConnection { implicit conn =>
    return Try(SQL("SELECT last_mapped_at FROM ip_asn_mappings ORDER BY last_mapped_at DESC LIMIT 1")()
      .map(_[Date]("last_mapped_at").getTime / 1000).head).getOrElse(0)
  }
  
  private def mapFromRow(row: SqlRow): Option[IpAsnMapping] = {
    return Try {
      IpAsnMapping(
      	row[Int]("id"), 
			  row[Long]("ip"),
			  row[Int]("asn"),
			  row[Date]("first_mapped_at").getTime / 1000,
			  row[Date]("last_mapped_at").getTime / 1000
      )
    }.toOption
  }
  
}

case class TopAs(asNum: Int, asName: String, numIps: Int, numUris: Int) {}