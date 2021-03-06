package models.cr

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._
import models.{Uri, UriSpec}
import controllers.PostgreSql

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
    
    "find CommunityReportSummaries" in {
      running(FakeApplication()) {
        val uri1 = validUri
        CommunityReport.create(uri1.id)
        val uri2 = validUri
        CommunityReport.create(uri2.id)
        val times = PostgreSql.parseTimes("")
        val recent = CommunityReport.findSummaries(None, None, times, 1000).map(_.uri)
        recent.nonEmpty must beTrue
        recent.contains(uri1.uri) must beTrue
        recent.contains(uri2.uri) must beTrue
        CommunityReport.findSummaries(None, None, times, 1).map(_.uri).contains(uri1.uri) must beFalse
        CommunityReport.findSummariesByUri(uri1.id).map(_.uri).contains(uri1.uri) must beTrue
      } 
  	}
    
    "find CommunityReportSummaries for CRs submitted since specified time" in {
      running(FakeApplication()) {
        val since = System.currentTimeMillis / 1000
        val uri1 = validUri
        CommunityReport.create(uri1.id)
        val uri2 = validUri
        CommunityReport.create(uri2.id)
        val summaries = CommunityReport.findSummariesSince(since)
        summaries.nonEmpty must beTrue
        val uris = summaries.map(_.uri)
        uris.contains(uri1.uri) must beTrue
        uris.contains(uri2.uri) must beTrue
      } 
  	}
    
  }  

}