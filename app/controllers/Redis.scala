package controllers

import java.net.URI
import play.api.Logger
import play.api.mvc.Controller
import com.codahale.jerkson.Json
import com.redis._
import models.enums.Source

object Redis extends Controller {
  
  private val googleRescanQueue = "GOOG_RESCAN_QUEUE"
  private val redisUrl = new URI(sys.env("REDIS_URL"))
  private lazy val redisPw = redisUrl.getUserInfo match {
    case s: String => Some(if (s.contains(":")) s.substring(s.indexOf(":")+1) else s)
    case _ => None
  }
  private lazy val pool = new RedisClientPool(redisUrl.getHost, redisUrl.getPort, secret=redisPw)
  
  def set(key: String, value: String): Boolean = pool.withClient(_.set(key, value))
  
  def get(key: String): Option[String] = pool.withClient(_.get(key))
  
  def drop(key: String): Boolean = pool.withClient(_.del(key)).getOrElse(0L) > 0L
  
  def addBlacklist(source: Source, time: Long, blacklist: List[String]): Boolean = {
    return pool.withClient(_.hset(source, time, Json.generate(blacklist)))
  }  
  
  def getBlacklist(source: Source, time: Long): List[String] = {
    val json = pool.withClient(_.hget(source, time))
    return if (json.isDefined) Json.parse[List[String]](json.get) else List()
  }
  
  def blacklistTimes(source: Source): List[Long] = {
    return pool.withClient(_.hkeys(source).getOrElse(List()).map(_.toLong))
  }
  
  def dropBlacklist(source: Source, time: Long): Long = {
    return pool.withClient(_.hdel(source, time).getOrElse(0L))
  }
  
  def addToGoogleRescanQueue(uri: String): Boolean = {
    return pool.withClient(_.sadd(googleRescanQueue, uri)) == Some(1)
  }  
  
  def getGoogleRescanQueue: Set[String] = {
    return pool.withClient(_.smembers(googleRescanQueue)).get.flatten
  }
  
  def removeFromGoogleRescanQueue(toRemove: Set[String]): Int = {
    return pool.withClient { client =>
      	toRemove.foldLeft(0) { (cnt, str) =>
      	cnt + client.srem(googleRescanQueue, str).get.toInt
      }
    }
  }

}