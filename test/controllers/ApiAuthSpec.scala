package controllers

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class ApiAuthSpec extends Specification {

  "ApiAuth" should {
    
    "generate a new key pair" in {
      running(FakeApplication()) {
        val pair = ApiAuth.newPair.get
        pair._1.isEmpty must beFalse
        pair._2.isEmpty must beFalse
        ApiAuth.dropPair(pair._1) must beTrue
      }
    }
    
    "delete a key pair" in {
      running(FakeApplication()) {
        val pair = ApiAuth.newPair.get
        ApiAuth.dropPair(pair._1) must beTrue
      }
    }
    
    "authenticate" in {
      running(FakeApplication()) {
        val pair = ApiAuth.newPair.get
        val ts = System.currentTimeMillis / 1000
        val path = "/for/the/horde"
        val sig = Hash.sha256(pair._1+ts+path+pair._2).get
        ApiAuth.authenticate(pair._1, ts, path, sig) must beTrue
        ApiAuth.authenticate(pair._1, 0L, path, sig) must beFalse
        ApiAuth.authenticate(pair._1, ts, "", sig) must beFalse
        ApiAuth.authenticate(pair._1, ts, path, Hash.sha256("").get) must beFalse
        ApiAuth.authenticate("", ts, path, sig) must beFalse
        ApiAuth.dropPair(pair._1) must beTrue
      }
    }
    
  }
  
}