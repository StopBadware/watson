package models.cr

import java.util.Date
import java.sql.{Timestamp, Types}
import anorm._
import play.api.db._
import play.api.Play.current
import play.api.Logger
import org.postgresql.util.PSQLException
import scala.util.Try
import controllers.PostgreSql

case class CommunityReport(
    id: Int,
    uriId: Int,
    ip: Option[Long],
    description: Option[String],
    badCode: Option[String],
    crTypeId: Option[Int],
    crSourceId: Option[Int],
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
  
  def create(
    uriId: Int,
    ip: Option[Long]=None,
    description: Option[String]=None,
    badCode: Option[String]=None,
    crTypeId: Option[Int]=None,
    crSourceId: Option[Int]=None
  ): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("""INSERT INTO community_reports (uri_id, ip, description, bad_code, cr_type_id, cr_source_id) 
        VALUES ({uriId}, {ip}, {description}, {badCode}, {crTypeId}, {crSourceId})""").on(
      		"uriId" -> uriId,
      		"ip" -> ip,
      		"description" -> description,
      		"badCode" -> badCode,
      		"crTypeId" -> crTypeId,
      		"crSourceId" -> crSourceId
        ).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def bulkCreate(
    uriIds: List[Int],
    ip: Option[Long]=None,
    description: Option[String]=None,
    badCode: Option[String]=None,
    crTypeId: Option[Int]=None,
    crSourceId: Option[Int]=None
  ): Int = DB.withTransaction { implicit conn =>
    return try {
      val sql = "INSERT INTO community_reports (uri_id, ip, description, bad_code, cr_type_id, cr_source_id) VALUES (?, ?, ?, ?, ?, ?)"
      val ps = conn.prepareStatement(sql)
      uriIds.foreach { uriId =>
        ps.setInt(1, uriId)
        if (ip.isDefined) ps.setLong(2, ip.get) else ps.setNull(2, Types.BIGINT)
        if (description.isDefined) ps.setString(3, description.get) else ps.setNull(3, Types.VARCHAR)
        if (badCode.isDefined) ps.setString(4, badCode.get) else ps.setNull(4, Types.VARCHAR)
        if (crTypeId.isDefined) ps.setInt(5, crTypeId.get) else ps.setNull(5, Types.INTEGER)
        if (crSourceId.isDefined) ps.setInt(6, crSourceId.get) else ps.setNull(6, Types.INTEGER)
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
  
  def findSummariesByUri(uriId: Int): List[CommunityReportSummary] = {
    findSummaries(None, None, PostgreSql.parseTimes(""), 5000, Some(uriId))
  }
  
  def findSummaries(
      crTypeId: Option[Int], 
      crSourceId: Option[Int], 
      times: (Timestamp, Timestamp), 
      limit: Int,
      uriId: Option[Int]=None): List[CommunityReportSummary] = DB.withConnection { implicit conn =>
    val sql = {
      """SELECT cr.id, uri, cr.ip, cr.description, cr_type, full_name, cr.reported_at FROM community_reports AS cr 
    		JOIN uris on cr.uri_id=uris.id LEFT JOIN cr_types ON cr.cr_type_id=cr_types.id LEFT JOIN cr_sources ON 
    		cr.cr_source_id=cr_sources.id WHERE reported_at BETWEEN {start} AND {end} """ +
    	(if (crTypeId.isDefined) "AND cr_type_id={crTypeId} " else "") +
    	(if (crSourceId.isDefined) "AND cr_source_id={crSourceId} " else "") +
    	(if (uriId.isDefined) "AND uri_id={uriId} " else "") +
  		"ORDER BY reported_at DESC LIMIT {limit}"
    }
    return Try(SQL(sql)
      .on("limit" -> limit, 
          "crTypeId" -> crTypeId,
          "crSourceId" -> crSourceId,
          "uriId" -> uriId,
          "start" -> times._1,
          "end" -> times._2
      )().map(CommunityReportSummary.mapFromRow).flatten.toList)
      .getOrElse(List.empty[CommunityReportSummary])
  }
  
  private def mapFromRow(row: SqlRow): Option[CommunityReport] = {
    return Try {
      CommunityReport(
      	row[Int]("id"), 
			  row[Int]("uri_id"),
			  row[Option[Long]]("ip"),
			  row[Option[String]]("description"),
			  row[Option[String]]("bad_code"),
			  row[Option[Int]]("cr_type_id"),
			  row[Option[Int]]("cr_source_id"),
			  row[Date]("reported_at").getTime / 1000
      )
    }.toOption
  }
  
}

case class CommunityReportSummary(
    id: Int,
    uri: String,
    ip: Option[Long],
    description: Option[String],
    crType: Option[String],
    crSource: Option[String],
    reportedAt: Long
  ) {}

object CommunityReportSummary {
  
  def mapFromRow(row: SqlRow): Option[CommunityReportSummary] = {
    return Try {
      CommunityReportSummary(
      	row[Int]("id"), 
			  row[String]("uri"),
			  row[Option[Long]]("ip"),
			  row[Option[String]]("description"),
			  row[Option[String]]("cr_type"),
			  row[Option[String]]("full_name"),
			  row[Date]("reported_at").getTime / 1000
      )
    }.toOption
  }
  
}