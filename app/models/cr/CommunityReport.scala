package models.cr

import java.util.Date
import java.sql.{Timestamp, Types}
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.Logger
import org.postgresql.util.PSQLException
import scala.util.Try

case class CommunityReport(
    id: Int,
    uriId: Int,
    ip: Option[Long],
    description: Option[String],
    badCode: Option[String],
    crTypeId: Option[Int],
    reportedVia: Option[Int],
    reportedAt: Long
  ) {
  
  def delete(): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("DELETE FROM community_reports WHERE id={id}").on("id" -> id).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }

}

object CommunityReport {
  
  def create(uriId: Int,
    ip: Option[Long]=None,
    description: Option[String]=None,
    badCode: Option[String]=None,
    crTypeId: Option[Int]=None,
    reportedVia: Option[Int]=None
  ): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("""INSERT INTO community_reports (uri_id, ip, description, bad_code, cr_type_id, reported_via) 
        VALUES ({uriId}, {ip}, {description}, {badCode}, {crTypeId}, {reportedVia})""").on(
      		"uriId" -> uriId,
      		"ip" -> ip,
      		"description" -> description,
      		"badCode" -> badCode,
      		"crTypeId" -> crTypeId,
      		"reportedVia" -> reportedVia
        ).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def bulkCreate(uriIds: List[Int],
    ip: Option[Long]=None,
    description: Option[String]=None,
    badCode: Option[String]=None,
    crTypeId: Option[Int]=None,
    reportedVia: Option[Int]=None
  ): Int = DB.withTransaction { implicit conn =>
    return try {
      val sql = "INSERT INTO community_reports (uri_id, ip, description, bad_code, cr_type_id, reported_via) VALUES (?, ?, ?, ?, ?, ?)"
      val ps = conn.prepareStatement(sql)
      uriIds.foreach { uriId =>
        ps.setInt(1, uriId)
        if (ip.isDefined) ps.setLong(2, ip.get) else ps.setNull(2, Types.BIGINT)
        if (description.isDefined) ps.setString(3, description.get) else ps.setNull(3, Types.VARCHAR)
        if (badCode.isDefined) ps.setString(4, badCode.get) else ps.setNull(4, Types.VARCHAR)
        if (crTypeId.isDefined) ps.setInt(5, crTypeId.get) else ps.setNull(5, Types.INTEGER)
        if (reportedVia.isDefined) ps.setInt(6, reportedVia.get) else ps.setNull(6, Types.INTEGER)
        ps.addBatch()
      }
      val batch = ps.executeBatch()
      batch.foldLeft(0)((cnt, b) => cnt + b)
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      0
    }
  }
  
  def find(id: Int): Option[CommunityReport] = DB.withConnection { implicit conn =>
    return Try(mapFromRow(SQL("SELECT * FROM community_reports WHERE id={id} LIMIT 1").on("id"->id)().head)).getOrElse(None)
  }
  
  def findByUri(uriId: Int): List[CommunityReport] = DB.withConnection { implicit conn =>
    return Try(SQL("SELECT * FROM community_reports WHERE uri_id={uriId}")
      .on("uriId" -> uriId)().map(mapFromRow).flatten.toList).getOrElse(List.empty[CommunityReport])
  }
  
  private def mapFromRow(row: SqlRow): Option[CommunityReport] = {
    return try {
      Some(CommunityReport(
      	row[Int]("id"), 
			  row[Int]("uri_id"),
			  row[Option[Long]]("ip"),
			  row[Option[String]]("description"),
			  row[Option[String]]("bad_code"),
			  row[Option[Int]]("cr_type_id"),
			  row[Option[Int]]("reported_via"),
			  row[Date]("reported_at").getTime / 1000
      ))
    } catch {
      case e: Exception => None
    }
  }
  
}