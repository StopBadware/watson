package models

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class RequestResponseSpec extends Specification {
  
  private def request: ReviewRequest = {
    val uri = Uri.findOrCreate(UriSpec.validUri).get
    ReviewRequest.create(uri.id, "valeera@example.com")
    ReviewRequest.findByUri(uri.id).head
  }
  
  private def question = {
    val text = System.currentTimeMillis.toHexString+System.nanoTime.toHexString
    RequestQuestion.create(text)
    RequestQuestion.findByText(text).get
  }
  
  private def answer = {
    val rq = question
    val text = System.nanoTime.toHexString.reverse
    RequestAnswer.create(text, rq.id)
    rq.answers.filter(_.answer.equals(text)).head
  }
  
  "RequestResponse" should {
    
    "create a RequestResponse" in {
      running(FakeApplication()) {
        RequestResponse.create(request.id, question.id, answer.id) must beTrue
      }
    }
    
    "find a RequestResponse" in {
      running(FakeApplication()) {
        val questionId = question.id
        RequestResponse.create(request.id, questionId, answer.id)
        val rr = RequestResponse.findByQuestion(questionId).head
        RequestResponse.find(rr.id) must beSome
      }
    }
    
    "find RequestResponses by question" in {
      running(FakeApplication()) {
        val questionId = question.id
        RequestResponse.create(request.id, questionId, answer.id)
        RequestResponse.findByQuestion(questionId).nonEmpty must beTrue
      }
    }
    
    "find RequestResponses by review request" in {
      running(FakeApplication()) {
        val requestId = request.id
        RequestResponse.create(requestId, question.id, answer.id)
        RequestResponse.findByReview(requestId).nonEmpty must beTrue
      }
    }
    
  }

}