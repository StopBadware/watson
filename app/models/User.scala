package models

import java.util.Date
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
      val newRoles = roles.filter(_ != role).map("'"+_+"'::ROLE").mkString(",")
      SQL("UPDATE users SET roles=ARRAY["+newRoles+"] WHERE id={id}").on("id"->id).executeUpdate() > 0
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def updateLoginCount(): Boolean = DB.withConnection { implicit conn =>
    return try {
      SQL("UPDATE users SET logins=(logins+1), last_login=NOW() WHERE id={id}").on("id"->id).executeUpdate() > 0
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
  
  def hasRole(role: Role): Boolean = roles.contains(role)
  
}

object User {
  
  def create(uname: String, emailAddress: String): Boolean = DB.withConnection { implicit conn =>
    return try {
      if (emailAddress.endsWith("@stopbadware.org")) {
      	SQL("INSERT INTO users (username, email, roles) VALUES ({username}, {email}, ARRAY['USER'::ROLE])")
      		.on("username" -> uname.toLowerCase, "email" -> emailAddress).executeUpdate() > 0
      } else {
        false
      }
    } catch {
      case e: PSQLException => Logger.error(e.getMessage)
      false
    }
  }
  
  def find(id: Int): Option[User] = DB.withConnection { implicit conn =>
    return Try(mapFromRow(SQL("SELECT * FROM users WHERE id={id} LIMIT 1").on("id"->id)().head)).getOrElse(None)
  }
  
  def findByUsername(uname: String): Option[User] = DB.withConnection { implicit conn =>
    return Try(mapFromRow(SQL("SELECT * FROM users WHERE username={uname} LIMIT 1").on("uname"->uname)().head)).getOrElse(None)
  }
  
  def findByEmail(email: String): Option[User] = DB.withConnection { implicit conn =>
    return Try(mapFromRow(SQL("SELECT * FROM users WHERE email={email} LIMIT 1").on("email"->email)().head)).getOrElse(None)
  }
  
  def all: List[User] = DB.withConnection { implicit conn =>
    return Try(SQL("SELECT * FROM users ORDER BY username ASC")().map(mapFromRow).flatten.toList).getOrElse(List())
  }
  
  private def mapFromRow(row: SqlRow): Option[User] = {
    val lastLogin = if (row[Option[Date]]("last_login").isDefined) {
      Some(row[Option[Date]]("last_login").get.getTime / 1000)
    } else {
      None
    }
    return Try {
      User(
      	row[Int]("id"), 
			  row[Option[Array[Role]]]("roles").getOrElse(Array()).toSet,
			  row[String]("username"),
			  row[String]("email"),
			  row[Int]("logins"),
			  lastLogin,
			  row[Date]("created_at").getTime / 1000
      )
    }.toOption
  }
  
}