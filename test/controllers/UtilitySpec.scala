package controllers

import java.net.URI
import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class UtilitySpec extends Specification {
  
  "Hash" should {
    
    "hash a string using SHA2-256" in {
      val string = "The quick brown fox jumps over the lazy dog"
      val hashed = "d7a8fbb307d7809469ca9abcb0082e4f8d5651e46d3cdb762d02d0bf37c9e592"
      Hash.sha256(string).get must equalTo(hashed)
    }
    
  }
  
  "Ip" should {
    
    val ips: Map[Long, String] = Map(
      0L -> "0.0.0.0",
      1L -> "0.0.0.1",
      256L -> "0.0.1.0",
      65536L -> "0.1.0.0",
      16777216L -> "1.0.0.0",
      17306635L -> "1.8.20.11",
      134744072L -> "8.8.8.8",
      2130706433L -> "127.0.0.1",
      4294967295L -> "255.255.255.255")
    
    "convert dotted quad to long" in {
      Ip.toLong("::ffff:1.1.1.1") must equalTo(Some(16843009))
      ips.map { case (ip, dots) =>
        Ip.toLong(dots) must equalTo(Some(ip))
      }
      val invalid = List("0", "0.0", "0.0.0", "0.0.0.0.0", "256.256.256.256", "0.0.0.256", "", "A.B.C.D")
      invalid.map(Ip.toLong(_) must beNone)
    }
    
    "convert long to dotted quad" in {
      ips.map { case (ip, dots) =>
        Ip.toDots(ip) must equalTo(Some(dots))
      }
      List(-1, 4294967296L).map(Ip.toDots(_) must beNone)
    }
    
  }
  
  "Host" should {
    
    "reverse the labels of a valid host" in {
      val normal = "www.example.com"
      val reversed = "com.example.www"
      Host.reverse(normal) must equalTo(reversed)
    }
    
    "reverse the labels of an invalid host" in {
      val reversed = "com.for_the_horde"
      Host.reverse(new URI("http://for_the_horde.com/path")) must equalTo(reversed)
      Host.reverse(new URI("http://for_the_horde.com")) must equalTo(reversed)
      Host.reverse(new URI("http://for_the_horde.com/")) must equalTo(reversed)
      Host.reverse(new URI("for_the_horde.com/path/")) must equalTo(reversed)
      Host.reverse(new URI("for_the_horde.com/path")) must equalTo(reversed)
    }    
    
  }
  
  "Email" should {
    
    "check if address is valid" in {
      Email.isValid("voljin@example.com") must beTrue
      Email.isValid("sylvanas.windrunner@email.example.co.uk") must beTrue
      Email.isValid("") must beFalse
      Email.isValid("bronzebeard") must beFalse
      Email.isValid("example.com") must beFalse
      Email.isValid("@example.com") must beFalse
      Email.isValid("bronzebeard@example") must beFalse
    }
    
  }
  
  "PostgreSql" should {
    
    "check if error is duplicate warning" in {
      PostgreSql.isNotDupeError("For the Horde!") must beTrue
      PostgreSql.isNotDupeError("ERROR: duplicate key value violates unique constraint") must beFalse
    }
    
  }

}