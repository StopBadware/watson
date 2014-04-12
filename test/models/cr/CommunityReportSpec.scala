package models.cr

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._
import models.{Uri, UriSpec}

@RunWith(classOf[JUnitRunner])
class CommunityReportSpec extends Specification {
  
  private def validUri: Uri = Uri.findOrCreate(UriSpec.validUri).get
  
  "CommunityReport" should {
    
    "create a CommunityReport" in {
      running(FakeApplication()) {
        CommunityReport.create(validUri.id) must beTrue
      } 
  	}
    
    "bulk create CommunityReports" in {
      running(FakeApplication()) {
        val numInBulk = 10
        val uris = (1 to numInBulk).foldLeft(List.empty[Int])((l, _) => validUri.id +: l)
        CommunityReport.bulkCreate(uris) must equalTo(numInBulk)
      } 
  	}
    
    "delete a CommunityReport" in {
      running(FakeApplication()) {
        val uriId = validUri.id
        CommunityReport.create(uriId)
        val cr = CommunityReport.findByUri(uriId).head
        cr.delete() must beTrue
        CommunityReport.findByUri(uriId).map(_.id).contains(cr.id) must beFalse
      } 
  	}
    
    "find a CommunityReport" in {
      running(FakeApplication()) {
        val uriId = validUri.id
        CommunityReport.create(uriId)
        val cr = CommunityReport.findByUri(uriId).head
        CommunityReport.find(cr.id) must beSome
      } 
  	}
    
  }  

}