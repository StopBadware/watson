package models.cr

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class CrSourceSpec extends Specification {
  
  sequential //run sequentially to ensure unique valid name
  
  private def validName = java.lang.Long.toString(System.currentTimeMillis, 36).substring(2)
  
  "CrSource" should {
    
    "create a CrSource" in {
      running(FakeApplication()) {
        CrSource.create(validName, validName) must beTrue
      } 
  	}
    
    "delete a CrSource" in {
      running(FakeApplication()) {
        val name = validName
        CrSource.create(name, name)
        val source = CrSource.findByName(name).get
        source.delete() must beTrue
        CrSource.findByName(name) must beNone
      } 
  	}
    
    "update a CrSource" in {
      running(FakeApplication()) {
        val name = validName
        CrSource.create(name, name)
        val source = CrSource.findByName(name).get
        source.update(name.reverse, name)
        CrSource.findByName(name) must beNone
        CrSource.findByName(name.reverse) must beSome
      } 
  	}
    
    "find a CrSource" in {
      running(FakeApplication()) {
        val name = validName
        CrSource.create(name, name)
        val source = CrSource.findByName(name)
        source must beSome
        CrSource.find(source.get.id) must beSome
      } 
  	}
    
    "return all short names" in {
      running(FakeApplication()) {
        val name = validName
        CrSource.create(name, name)
        val all = CrSource.all
        all.nonEmpty must beTrue
        all.contains(name) must beTrue
      } 
  	}
    
  }  

}