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

}