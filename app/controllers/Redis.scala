package controllers

import java.net.URI
import play.api.Logger
import play.api.mvc.Controller
import com.redis._

object Redis extends Controller {
  
  private val redisUrl = new URI(sys.env("REDIS_URL"))
  private lazy val pool = new RedisClientPool(redisUrl.getHost, redisUrl.getPort)
  
  try {
    redisUrl.getUserInfo match {
      case s: String => {
        val userInfo = if (s.startsWith(":")) s.substring(1) else s
        pool.withClient(client => client.auth(userInfo))
      }
      case _ =>
    }
  } catch {
    case e: Exception => Logger.error("Unable to authenticate Redis pool")
  }
  
  def set(key: String, value: String): Boolean = {
    return pool.withClient(client => client.set(key, value))
  }
  
  def get(key: String): Option[String] = {
    return pool.withClient(client => client.get(key))
  }
  
  def addToSet(set: String, value: String): Long = {
    return pool.withClient(client => client.sadd(set, value).getOrElse(0L))
  }
  
  def setContains(set: String, value: String): Boolean = {
    return pool.withClient(client => client.sismember(set, value))
  }
  
  def getSet(set: String): Set[Option[String]] = {
    return pool.withClient(client => client.smembers(set).getOrElse(Set()))
  }
  
  def addToMap(map: String, field: String, value: String): Boolean = {
    return pool.withClient(client => client.hset(map, field, value))
  }
  
  def mapKeys(map: String): List[String] = {
    return pool.withClient(client => client.hkeys(map).getOrElse(List()))
  }
  
  def getFromMap(map: String, field: String): Option[String] = {
    return pool.withClient(client => client.hget(map, field))
  }
  
  def delFromMap(map: String, field: String): Long = {
    return pool.withClient(client => client.hdel(map, field).getOrElse(0L))
  }

}