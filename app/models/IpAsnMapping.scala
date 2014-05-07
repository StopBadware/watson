package models

import java.util.Date
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.Logger
import org.postgresql.util.PSQLException
import scala.util.Try

case class IpAsnMapping(id: Int, ip: Long, asn: Int, mappedAt: Long) {

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
  
  def create(ip: Long, asn: Int): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("""INSERT INTO ip_asn_mappings (ip, asn) SELECT {ip}, {asn} WHERE NOT EXISTS (SELECT 1 FROM 
        (SELECT asn FROM ip_asn_mappings WHERE ip={ip} ORDER BY mapped_at DESC LIMIT 1) AS asn WHERE asn={asn} LIMIT 1)""")
        .on("ip" -> ip, "asn" -> asn).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def find(id: Int): Option[IpAsnMapping] = DB.withConnection { implicit conn =>
    return Try(mapFromRow(SQL("SELECT * FROM ip_asn_mappings WHERE id={id} LIMIT 1")
      .on("id" -> id)().head)).getOrElse(None)
  }
  
  def findByIp(ip: Long): List[IpAsnMapping] = DB.withConnection { implicit conn =>
    return Try(SQL("SELECT * FROM ip_asn_mappings WHERE ip={ip} ORDER BY mapped_at DESC")
      .on("ip" -> ip)().map(mapFromRow).flatten.toList).getOrElse(List())
  }
  
  def findByAsn(asn: Int): List[IpAsnMapping] = DB.withConnection { implicit conn =>
    return Try(SQL("SELECT * FROM ip_asn_mappings WHERE asn={asn} ORDER BY mapped_at DESC")
      .on("asn" -> asn)().map(mapFromRow).flatten.toList).getOrElse(List())
  }
  
  private def mapFromRow(row: SqlRow): Option[IpAsnMapping] = {
    return Try {
      IpAsnMapping(
      	row[Int]("id"), 
			  row[Long]("ip"),
			  row[Int]("asn"),
			  row[Date]("mapped_at").getTime / 1000
      )
    }.toOption
  }
  
}