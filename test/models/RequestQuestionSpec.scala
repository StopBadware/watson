package models

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class RequestQuestionSpec extends Specification {
  
  private def question = {
    val text = System.currentTimeMillis.toHexString+System.nanoTime.toHexString
    RequestQuestion.create(text)
    RequestQuestion.findByText(text).get
  }
  
  "RequestQuestion" should {
    
    "create a RequestQuestion" in {
      running(FakeApplication()) {
        RequestQuestion.create(System.currentTimeMillis.toHexString) must beTrue
      }
    }
    
    "disable/enable a RequestQuestion" in {
      running(FakeApplication()) {
        val rq = question
        rq.enabled must beTrue
        rq.disable must beTrue
        RequestQuestion.find(rq.id).get.enabled must beFalse
        RequestQuestion.find(rq.id).get.enable must beTrue
        RequestQuestion.find(rq.id).get.enabled must beTrue
      }
    }
    
    "update a RequestQuestion" in {
      running(FakeApplication()) {
        val rq = question
        val newText = System.nanoTime.toHexString
        rq.question.equals(newText) must beFalse
        rq.update(newText) must beTrue
        RequestQuestion.find(rq.id).get.question.equals(newText) must beTrue
      }
    }
    
    "find a RequestQuestion" in {
      running(FakeApplication()) {
        RequestQuestion.find(question.id) must beSome
      }
    }
    
    "find all enabled RequestQuestions" in {
      running(FakeApplication()) {
        val rq = question
        rq.enabled must beTrue
        val enabled = RequestQuestion.enabled
        enabled.nonEmpty must beTrue
        enabled.map(_.id).contains(rq.id) must beTrue
      }
    }
    
  }

}