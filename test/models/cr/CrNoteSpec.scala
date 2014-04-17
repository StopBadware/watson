package models.cr

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._
import models.{Uri, UriSpec, User}

@RunWith(classOf[JUnitRunner])
class CrNoteSpec extends Specification {
  
  private def crId: Int = {
    val uriId = Uri.findOrCreate(UriSpec.validUri).get.id
    CommunityReport.create(uriId)
    CommunityReport.findByUri(uriId).head.id
  }
  
  private val author = {
    running(FakeApplication()) {
	    val testEmail = sys.env("TEST_EMAIL")
		  if (User.findByEmail(testEmail).isEmpty) {
		    User.create(testEmail.split("@").head, testEmail)
		  }
	    User.findByEmail(testEmail).get.id
    }
  }
  
  "CrNote" should {
    
    "create a CrNote" in {
      running(FakeApplication()) {
        CrNote.create(crId, author, "FOR THE HORDE!")
      } 
  	}
    
    "delete a CrNote" in {
      running(FakeApplication()) {
        val id = crId
        CrNote.create(id, author, "For the Forsaken!")
        val note = CrNote.find(CrNote.findByCr(id).head.id).get
        note.delete() must beTrue
        CrNote.find(note.id) must beNone
      } 
  	}
    
    "find a CrNote" in {
      running(FakeApplication()) {
        val id = crId
        CrNote.create(id, author, "For the Forsaken!")
        val note = CrNote.findByCr(id).headOption
        note must beSome
        CrNote.find(note.get.id) must beSome
      } 
  	}
    
    "find CrNotes with author name" in {
      running(FakeApplication()) {
        val id = crId
        CrNote.create(id, author, "For the Forsaken!")
        val note = CrNote.findByCr(id).head
        val notes = CrNote.findByCr(id)
        notes.nonEmpty must beTrue
        notes.map(_.id).contains(note.id) must beTrue
      } 
  	}
    
  }  

}