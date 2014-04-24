package models

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class EmailTemplateSpec extends Specification {
  
  val testSubject = "Lok'tar ogar"
  val testBody = "<h1>TEST</h1><p>Thrall says: <a href=\"http://example.com/\">For the Horde!</a></p>"
  def nowHex: String = System.currentTimeMillis.toHexString
  
  "EmailTemplate" should {
    
    "create a template" in {
      running(FakeApplication()) {
        val name = "TEST_CRT_" + nowHex
        EmailTemplate.find(name) must beNone
        EmailTemplate.create(name, testSubject, testBody, None) must beTrue
        EmailTemplate.find(name) must beSome
      }
    }
    
    "update a template" in {
      running(FakeApplication()) {
        val now = nowHex
        val name = "TEST_UPD_" + now
        EmailTemplate.create(name, testSubject, testBody) must beTrue
        val et = EmailTemplate.find(name).get
        et.subject must equalTo(testSubject)
        et.body must equalTo(testBody)
        val newSubject = "NEW SUBJECT" + now
        val newBody = "NEW BODY NOW WITH 300% MORE CAPS!11!!1111!!1!" + now
        et.update(newSubject, newBody) must beTrue
        val updated = EmailTemplate.find(name).get
        updated.subject must not equalTo(testSubject)
        updated.subject must equalTo(newSubject)
        updated.body must not equalTo(testBody)
        updated.body must equalTo(newBody)
      }
    }
    
    "find a template" in {
      running(FakeApplication()) {
        val name = "TEST_FND_" + nowHex
        EmailTemplate.create(name, testSubject, testBody) must beTrue
        EmailTemplate.find(name) must beSome
      }
    }
    
    "find all templates" in {
      running(FakeApplication()) {
        val name = "TEST_ALL_" + nowHex
        EmailTemplate.create(name, testSubject, testBody) must beTrue
        val all = EmailTemplate.all
        all.nonEmpty must beTrue
        all.map(_.name).contains(name) must beTrue
      }
    }
    
    "delete a template" in {
      running(FakeApplication()) {
        val name = "TEST_DEL_" + nowHex
        EmailTemplate.create(name, testSubject, testBody) must beTrue
        val et = EmailTemplate.find(name)
        et must beSome
        et.get.delete() must beTrue
        EmailTemplate.find(name) must beNone
      }
    }
    
  }

}