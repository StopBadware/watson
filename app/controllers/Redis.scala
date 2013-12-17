package controllers

import java.net.URI
import play.api.Logger
import play.api.mvc.Controller
import com.codahale.jerkson.Json
import com.redis._
import models.Source

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
  
  def addBlacklist(source: Source, time: Long, blacklist: List[String]): Boolean = {
    return pool.withClient(client => client.hset(source, time, Json.generate(blacklist)))
  }  
  
  def getBlacklist(source: Source, time: Long): List[String] = {
    val json = pool.withClient(client => client.hget(source, time))
    return if (json.isDefined) Json.parse[List[String]](json.get) else List()
  }
  
  def blacklistTimes(source: Source): List[Long] = {
    return pool.withClient(client => client.hkeys(source).getOrElse(List()).map(_.toLong))
  }
  
  def dropBlacklist(source: Source, time: Long): Long = {
    return pool.withClient(client => client.hdel(source, time).getOrElse(0L))
  }

}