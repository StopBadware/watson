package controllers

import java.math.BigInteger
import java.security.MessageDigest

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

}