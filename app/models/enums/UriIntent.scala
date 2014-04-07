package models.enums

import anorm.{Column, TypeDoesNotMatch}
import anorm.MayErr.eitherToError
import org.postgresql.jdbc4.Jdbc4Array

protected class UriIntent(uriIntent: String) {
	override def toString: String = uriIntent
}

object UriIntent {
  
  val FREE_HOST: UriIntent = models.enums.FREE_HOST
  val HACKED: UriIntent = models.enums.HACKED
  val MALICIOUS: UriIntent = models.enums.MALICIOUS
  
  val intents = Map(
      "FREE_HOST" -> FREE_HOST,
      "HACKED" -> HACKED,
      "MALICIOUS" -> MALICIOUS
    )
  
  def fromStr(str: String): Option[UriIntent] = {
    val upper = str.toUpperCase
    return if (intents.contains(upper)) Some(intents(upper)) else None
  }
  
  implicit def rowToUriIntent: Column[UriIntent] = {
    Column.nonNull[UriIntent] { (value, meta) =>
      val status = UriIntent.fromStr(value.toString)
	    if (status.isDefined) Right(status.get) else Left(TypeDoesNotMatch(value.toString+" - "+meta))
    }
  }
  
}

case object FREE_HOST extends UriIntent("FREE_HOST")
case object HACKED extends UriIntent("HACKED")
case object MALICIOUS extends UriIntent("MALICIOUS")
