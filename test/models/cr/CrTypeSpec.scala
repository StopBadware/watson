package models.cr

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class CrTypeSpec extends Specification {
  
  sequential //run sequentially to ensure unique valid type
  
  private def validType = java.lang.Long.toString(System.currentTimeMillis, 36).substring(2)
  
  "CrType" should {
    
    "create a CrType" in {
      running(FakeApplication()) {
        CrType.create(validType) must beTrue
      } 
  	}
    
    "delete a CrType" in {
      running(FakeApplication()) {
        val name = validType
        CrType.create(name) must beTrue
        val crt = CrType.findByType(name).get
        crt.delete() must beTrue
        CrType.find(crt.id) must beNone
      } 
  	}
    
    "update a CrType" in {
      running(FakeApplication()) {
        val name = validType
        CrType.create(name) must beTrue
        val crt = CrType.findByType(name).get
        crt.update(name.reverse) must beTrue
        CrType.findByType(name) must beNone
        CrType.findByType(name.reverse) must beSome
      } 
  	}
    
    "find a CrType" in {
      running(FakeApplication()) {
        val name = validType
        CrType.create(name) must beTrue
        val crt = CrType.findByType(name)
        crt must beSome
        CrType.find(crt.get.id) must beSome
      } 
  	}
    
    "return all type names" in {
      running(FakeApplication()) {
        val name = validType
        CrType.create(name) must beTrue
        val all = CrType.all
        all.nonEmpty must beTrue
        all.contains(name) must beTrue
      } 
  	}
    
  }

}