package models.enums

import anorm.{Column, TypeDoesNotMatch}
import anorm.MayErr.eitherToError
import org.postgresql.jdbc4.Jdbc4Array

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
  
  implicit def rowToRoleArray: Column[Array[Role]] = {
    Column.nonNull[Array[Role]] { (value, meta) =>
      try {
      	Right(value.asInstanceOf[Jdbc4Array].getArray().asInstanceOf[Array[Object]]
    			.map(r=>Role.fromStr(r.toString)).flatten)
      } catch {
        case _: Exception => Left(TypeDoesNotMatch(value.toString+" - "+meta))
      }
    }
  }
  
}

case object USER extends Role("USER")
case object REVIEWER extends Role("REVIEWER")
case object VERIFIER extends Role("VERIFIER")
case object ADMIN extends Role("ADMIN")
