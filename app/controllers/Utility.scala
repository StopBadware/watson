package controllers

import java.math.BigInteger
import java.net.URI
import java.security.MessageDigest
import org.postgresql.jdbc4.Jdbc4Array
import anorm._

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
  
}