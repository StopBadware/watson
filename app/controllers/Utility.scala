package controllers

import java.math.BigInteger
import java.net.URI
import java.security.MessageDigest
import java.sql.Timestamp
import java.text.{ParseException, SimpleDateFormat}
import scala.util.Try
import anorm._
import play.api.mvc._
import org.postgresql.jdbc4.Jdbc4Array
import com.fasterxml.jackson.databind.{JsonNode, JsonMappingException, ObjectMapper}
import com.fasterxml.jackson.core.JsonParseException

object Hash {
  
  def sha256(msg: String): Option[String] = {
    return try {
      val md = MessageDigest.getInstance("SHA-256").digest(msg.getBytes)
      val sha2 = (new BigInteger(1, md)).toString(16)
      Some(sha2.format("%64s", sha2).replace(' ', '0'))
    } catch {
      case e: Exception => None
    }
  }
  
}

object Host {
  
  def reverse(host: String): String = host.split("\\.").reverse.mkString(".")
  
  def reverse(uri: URI): String = {
    val host = Option(uri.getHost)
    return reverse(if (host.isEmpty) {
      val str = uri.toString
      val begin = if (str.indexOf("//") > 0) str.indexOf("//") + 2 else 0
      val end = if (str.indexOf("/", begin) > 0) str.indexOf("/", begin) else str.length
      str.substring(begin, end)
    } else {
      host.get
    })
  }  

}

object Email {
  
  def isValid(email: String): Boolean = email.matches("^.+@([a-zA-Z0-9\\-\\_]+\\.)*[a-zA-Z0-9\\-\\_]+\\.[a-zA-Z]+$")
  
}

object PostgreSql {
  
  def isNotDupeError(err: String): Boolean = !err.startsWith("ERROR: duplicate key")
  
  implicit def rowToIntArray: Column[Array[Int]] = {
    Column.nonNull[Array[Int]] { (value, meta) =>
      try {
      	Right(value.asInstanceOf[Jdbc4Array].getArray().asInstanceOf[Array[Object]].map(_.asInstanceOf[Int]))
      } catch {
        case _: Exception => Left(TypeDoesNotMatch(value.toString+" - "+meta))
      }
    }
  }
  
  def toTimestamp(date: String, format: String): Option[Timestamp] = {
    val df = new SimpleDateFormat(format)
    return try {
      Some(new Timestamp(df.parse(date).getTime))
    } catch {
      case e: ParseException => None
    }
  }
  
}

trait Cookies {
  
  def cookies(request: Request[AnyContent], fields: List[String]): Seq[Cookie] = { 
    fields.map { field =>
    	cookie(field, request.getQueryString(field).getOrElse(""))
    }
  }
  
  private def cookie(key: String, value: String): Cookie = {
    Cookie(key, value, None, "/", None, false, false)
  }
  
}

trait JsonMapper {
  
  def mapJson(txt: String): Option[JsonNode] = Try(Some((new ObjectMapper).readTree(txt))).getOrElse(None)
  
}