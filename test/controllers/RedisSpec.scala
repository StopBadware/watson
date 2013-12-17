package controllers

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._
import scala.util.Random
import models.Source

@RunWith(classOf[JUnitRunner])
class RedisSpec extends Specification {
  
  private val source = Source.GOOG
  private val url = "example.com"
  
  "Redis" should {
    
    "add a blacklist" in {
      Redis.addBlacklist(source, System.currentTimeMillis + Random.nextInt, List(url)) must beTrue
    }
    
    "retrieve a blacklist" in {
      val field = System.currentTimeMillis + Random.nextInt
      Redis.addBlacklist(source, field, List(url)) must beTrue
      val blacklist = Redis.getBlacklist(source, field)
      blacklist.nonEmpty must beTrue
      blacklist.contains(url) must beTrue
    }
    
    "delete a blacklist" in {
      val field = System.currentTimeMillis + Random.nextInt
      Redis.addBlacklist(source, field, List(url)) must beTrue
      Redis.getBlacklist(source, field).nonEmpty must beTrue
      Redis.dropBlacklist(source, field)
      Redis.getBlacklist(source, field).isEmpty must beTrue
    }
    
    "retrieve blacklist times" in {
      val field = System.currentTimeMillis + Random.nextInt
      Redis.addBlacklist(source, field, List(url)) must beTrue
      Redis.blacklistTimes(source).contains(field) must beTrue
    }
    
  }

}