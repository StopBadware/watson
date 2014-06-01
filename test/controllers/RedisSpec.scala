package controllers

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import org.specs2.specification.AfterExample
import play.api.test._
import play.api.test.Helpers._
import scala.util.Random
import models.enums.Source

@RunWith(classOf[JUnitRunner])
class RedisSpec extends Specification with AfterExample {
  
  sequential	//running in parallel can cause tests to overwrite each other (or be removed while still needed)
  private val source = Source.GOOG
  private val url = "example.com"
  private val resolveHosts = List(
    "example.com", 
    "example"+System.nanoTime.toHexString+".com",
    "example"+System.currentTimeMillis.toHexString+".com"
  )
    
  def after = Redis.blacklistTimes(source).foreach(Redis.dropBlacklist(source, _))
  
  "Redis" should {
    
    "add a blacklist" in {
      Redis.addBlacklist(source, System.currentTimeMillis / 1000, List(url)) must beTrue
    }
    
    "retrieve a blacklist" in {
      val field = System.currentTimeMillis / 1000
      Redis.addBlacklist(source, field, List(url)) must beTrue
      val blacklist = Redis.getBlacklist(source, field)
      blacklist.nonEmpty must beTrue
      blacklist.contains(url) must beTrue
    }
    
    "delete a blacklist" in {
      val field = System.currentTimeMillis / 1000
      Redis.addBlacklist(source, field, List(url)) must beTrue
      Redis.getBlacklist(source, field).nonEmpty must beTrue
      Redis.dropBlacklist(source, field)
      Redis.getBlacklist(source, field).isEmpty must beTrue
    }
    
    "retrieve blacklist times" in {
      val field = System.currentTimeMillis / 1000
      Redis.addBlacklist(source, field, List(url)) must beTrue
      Redis.blacklistTimes(source).contains(field) must beTrue
    }
    
    "add to Google rescan queue" in {
      Redis.addToGoogleRescanQueue(System.nanoTime.toHexString) must beTrue
    }
    
    "remove from Google rescan queue" in {
      val str = System.nanoTime.toHexString
      Redis.addToGoogleRescanQueue(str) must beTrue
      Redis.removeFromGoogleRescanQueue(Set(str)) must equalTo(1)
      Redis.getGoogleRescanQueue.contains(str) must beFalse
    }
    
    "retrieve Google rescan queue" in {
      val str = System.nanoTime.toHexString
      Redis.addToGoogleRescanQueue(str) must beTrue
      val queue = Redis.getGoogleRescanQueue
      queue.nonEmpty must beTrue
      queue.contains(str) must beTrue
    }
    
    "add to resolver queue" in {
      Redis.setResolverQueue(resolveHosts) must beTrue
    }
    
    "get resolver queue" in {
      val hosts = Redis.getResolverQueue
      hosts.nonEmpty must beTrue
      resolveHosts.map(host => hosts.contains(host) must beTrue)
    }
    
  }

}