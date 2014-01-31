package models

import anorm._
import play.api.db._
import play.api.Play.current
import play.api.Logger
import scala.util.Try
import org.postgresql.util.PSQLException
import models.enums.Role

case class User(
	id: Int,
  roles: Set[Role],
  username: String,
  email: String,
  logins: Int,
  lastLogin: Option[Long],
  createdAt: Long  
	) {
  
  def addRole(role: Role): Boolean = DB.withConnection { implicit conn =>
    return try {
      if (roles.contains(role)) {
        false
      } else {
        SQL("""UPDATE users SET roles=((SELECT roles FROM users WHERE id={id}) || ARRAY[{role}::ROLE]) 
          WHERE id={id}""").on("id" -> id, "role" -> role.toString).executeUpdate() > 0
      }
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def removeRole(role: Role): Boolean = DB.withConnection { implicit conn =>
    try {
      val roles = Try(SQL("SELECT roles FROM users WHERE id={id}").on("id" -> id)()
        .head[Option[Array[Role]]]("roles").getOrElse(Array()).toSet).getOrElse(Set())
      val newRoles = roles.filter(_ != role).mkString(",")
      SQL("UPDATE users SET roles=ARRAY["+newRoles+"] WHERE id={id}").on("id"->id).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def delete(): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("DELETE FROM users WHERE id={id}").on("id" -> id).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
}

object User {
  
  def create(uname: String, emailAddress: String): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("INSERT INTO users (username, email) VALUES ({username}, {email})")
      	.on("username" -> uname, "email" -> emailAddress).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
}