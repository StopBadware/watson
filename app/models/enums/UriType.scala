package models.enums

import anorm.{Column, TypeDoesNotMatch}
import anorm.MayErr.eitherToError
import org.postgresql.jdbc4.Jdbc4Array

protected class UriType(uriType: String) {
	override def toString: String = uriType
}

object UriType {
  
  val INTERMEDIARY: UriType = models.enums.INTERMEDIARY
  val LANDING: UriType = models.enums.LANDING
  val PAYLOAD: UriType = models.enums.PAYLOAD
  
  val types = Map(
      "INTERMEDIARY" -> INTERMEDIARY,
      "LANDING" -> LANDING,
      "PAYLOAD" -> PAYLOAD
    )
  
  def fromStr(str: String): Option[UriType] = {
    val upper = str.toUpperCase
    return if (types.contains(upper)) Some(types(upper)) else None
  }
  
  implicit def rowToUriType: Column[UriType] = {
    Column.nonNull[UriType] { (value, meta) =>
      val status = UriType.fromStr(value.toString)
	    if (status.isDefined) Right(status.get) else Left(TypeDoesNotMatch(value.toString+" - "+meta))
    }
  }
  
}

case object INTERMEDIARY extends UriType("INTERMEDIARY")
case object LANDING extends UriType("LANDING")
case object PAYLOAD extends UriType("PAYLOAD")
