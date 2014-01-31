package models.enums

import anorm.{Column, TypeDoesNotMatch}
import anorm.MayErr.eitherToError

protected class Role(role: String) {
	override def toString: String = role
}

object Role {
  
  val USER: Role = models.enums.USER
  val REVIEWER: Role = models.enums.REVIEWER
  val VERIFIER: Role = models.enums.VERIFIER
  val ADMIN: Role = models.enums.ADMIN
  
  val roles = Map(
      "USER" -> USER,
      "REVIEWER" -> REVIEWER,
      "VERIFIER" -> VERIFIER,
      "ADMIN" -> ADMIN
    )
  
  def fromStr(str: String): Option[Role] = {
    val upper = str.toUpperCase
    return if (roles.contains(upper)) Some(roles(upper)) else None
  }
  
  implicit def rowToSource: Column[Role] = {
    Column.nonNull[Role] { (value, meta) =>
      val role = Role.fromStr(value.toString)
	    if (role.isDefined) Right(role.get) else Left(TypeDoesNotMatch(value.toString+" - "+meta))
    }
  }  
  
}

case object USER extends Role("USER")
case object REVIEWER extends Role("REVIEWER")
case object VERIFIER extends Role("VERIFIER")
case object ADMIN extends Role("ADMIN")
