package controllers

import java.math.BigInteger
import java.net.URI
import java.security.MessageDigest
import java.sql.Timestamp
import java.text.{ParseException, SimpleDateFormat}
import scala.util.Try
import sun.misc.BASE64Encoder
import anorm._
import play.api.mvc._
import org.postgresql.jdbc4.Jdbc4Array
import com.fasterxml.jackson.databind.{JsonNode, JsonMappingException, ObjectMapper}
import com.fasterxml.jackson.core.JsonParseException

object Hash {
  
  def sha256(msg: String): Option[String] = {
    return Try {
      val md = MessageDigest.getInstance("SHA-256").digest(msg.getBytes)
      val sha2 = (new BigInteger(1, md)).toString(16)
      sha2.format("%64s", sha2).replace(' ', '0')
    }.toOption
  }
  
}

object Ip {
  
  def toLong(dots: String): Option[Long] = {
    return try {
      val ip = if (dots.startsWith("::ffff:")) dots.split("::ffff:").last else dots
      val octets = ip.split("\\.").map(_.toLong).toList
      if (octets.size == 4 && octets.forall(o => o >= 0 && o <= 255)) {
      	Some((octets(0)*16777216) + (octets(1)*65536) + (octets(2)*256) + octets(3))
      } else {
        None
      }
    } catch {
      case e: Exception => None
    }
  }
  
  def toDots(ip: Int): Option[String] = toDots(ip.toLong) 
  
  def toDots(ip: Long): Option[String] = {
  	return if (ip >= 0 && ip <= 4294967295L) {
  	  Try {val octets = List(
	      (ip >> 24) & 0xFF,
  	    (ip >> 16) & 0xFF,
	      (ip >> 8) & 0xFF,
	      ip & 0xFF)
  	  octets.mkString(".")
  	  }.toOption
  	} else {
  	  None
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

object Text {
  
  def truncate(str: String, max: Int): String = if (str.length > max) str.slice(0, max)+"..." else str
  
  def encodeBase64(str: String): String = new BASE64Encoder().encode(str.getBytes)
  
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
  
  implicit def rowToStringArray: Column[Array[String]] = {
    Column.nonNull[Array[String]] { (value, meta) =>
      try {
      	Right(value.asInstanceOf[Jdbc4Array].getArray().asInstanceOf[Array[Object]].map(_.asInstanceOf[String]))
      } catch {
        case _: Exception => Left(TypeDoesNotMatch(value.toString+" - "+meta))
      }
    }
  }  
  
  def toTimestamp(date: String): Option[Timestamp] = {
    val df = new SimpleDateFormat(Consts.FromStrFormat)
    return Try(new Timestamp(df.parse(date).getTime)).toOption
  }
  
  def parseTimes(datesStr: String): (Timestamp, Timestamp) = {
    val dates = datesStr.split("-").map(_.trim)
    val timestamps = dates.size match {
      case 1 => Try(Some(toTimestamp(dates(0)+Consts.StartOfDayTime).get, toTimestamp(dates(0)+Consts.EndOfDayTime).get)).getOrElse(None)
      case 2 => Try(Some(toTimestamp(dates(0)+Consts.StartOfDayTime).get, toTimestamp(dates(1)+Consts.EndOfDayTime).get)).getOrElse(None)
      case _ => None
    }
    return timestamps.getOrElse((Consts.TimeZero, Consts.TimeFuture))
  }
  
  private object Consts {
    val TimeZero = new Timestamp(0)
    val TimeFuture = new Timestamp(9999999999999L)
    val StartOfDayTime = "T00:00:00:000"
    val EndOfDayTime = "T23:59:59:999"
    val FromStrFormat = "dd MMM yyyy'T'HH:mm:ss:SSS"
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