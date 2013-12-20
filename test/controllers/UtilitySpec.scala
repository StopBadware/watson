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
  
  "PostgreSql" should {
    
    "check if error is duplicate warning" in {
      PostgreSql.isNotDupeError("For the Horde!") must beTrue
      PostgreSql.isNotDupeError("ERROR: duplicate key value violates unique constraint") must beFalse
    }
    
  }

}