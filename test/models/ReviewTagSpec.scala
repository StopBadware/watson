package models

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class ReviewTagSpec extends Specification {
  
  private val numInBulk = 5
  def nowHex: String = System.currentTimeMillis.toHexString
  def createAndFind(prefix: String): ReviewTag = {
    val name = prefix+"_"+nowHex
    ReviewTag.create(name)
    ReviewTag.findByName(name).get
  }
  
  "ReviewTag" should {
    
    "create a new tag" in {
      running(FakeApplication()) {
        ReviewTag.create("CRT_"+nowHex) must beTrue
      }
    }
    
    "update an existing tag" in {
      running(FakeApplication()) {
        val name = "UPD_"+nowHex
        ReviewTag.create(name, Some("Orgrimmar"), "000000")
        val tag = ReviewTag.findByName(name)
        val newDesc = "Undercity"+nowHex
        val newColor = "FFFFFF"
        tag.get.update(Some(newDesc), newColor) must beTrue
        val findAgain = ReviewTag.findByName(name).get
        findAgain.description.get must equalTo(newDesc)
        findAgain.hexColor must equalTo(newColor)
      }
    }
    
    "delete a tag" in {
      running(FakeApplication()) {
        val tag = createAndFind("DEL")
        tag.delete() must beTrue
        ReviewTag.find(tag.id) must beNone
      }
    }
    
    "de-activate a tag" in {
      running(FakeApplication()) {
        val tag = createAndFind("DEA")
        tag.active must beTrue
        tag.toggleActive(false) must beTrue
        ReviewTag.find(tag.id).get.active must beFalse
      }
    }
    
    "find a tag by id" in {
      running(FakeApplication()) {
        val tag = createAndFind("FID")
        ReviewTag.find(tag.id) must beSome
      }
    }
    
    "find tags by ids" in {
      running(FakeApplication()) {
        val ids = (1 to numInBulk).map(id => createAndFind("ID"+id).id).toList
        val found = ReviewTag.find(ids)
        found.size must equalTo(ids.size)
        found.map(tag => ids.contains(tag.id) must beTrue)
      }
    }
    
    "find all active tags" in {
      running(FakeApplication()) {
        val active = (1 to numInBulk).map(id => createAndFind("ID"+id).id).toList
        val inactive = (1 to numInBulk).map { id => 
          val tag = createAndFind("ID"+id)
          tag.toggleActive(false)
          tag.id
        }.toList
        val activeTags = ReviewTag.allActive.map(_.id)
        active.map(activeTags.contains(_) must beTrue)
        inactive.map(activeTags.contains(_) must beFalse)
      }
    }
    
    "find categories" in {
      running(FakeApplication()) {
        ReviewTag.categories.nonEmpty must beTrue
      }
    }
    
    "find a tag by name" in {
      running(FakeApplication()) {
        val tag = createAndFind("FNM")
        ReviewTag.findByName(tag.name) must beSome
        ReviewTag.findByName(tag.name.toUpperCase) must beSome
        ReviewTag.findByName(tag.name.toLowerCase) must beSome
      }
    }
    
  }

}