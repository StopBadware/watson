package models

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class RequestAnswerSpec extends Specification {
  
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
  
  "Request" should {
    
    "create a RequestAnswer" in {
      running(FakeApplication()) {
        RequestAnswer.create(System.currentTimeMillis.toHexString, question.id) must beTrue
      }
    }
    
    "disable/enable a RequestAnswer" in {
      running(FakeApplication()) {
        val ra = answer
        ra.enabled must beTrue
        ra.disable
        RequestAnswer.find(ra.id).get.enabled must beFalse
        RequestAnswer.find(ra.id).get.enable must beTrue
        RequestAnswer.find(ra.id).get.enabled must beTrue
      }
    }
    
    "update a RequestAnswer" in {
      running(FakeApplication()) {
        val ra = answer
        val newText = System.nanoTime.toHexString
        ra.answer.equals(newText) must beFalse
        ra.update(newText) must beTrue
        RequestAnswer.find(ra.id).get.answer.equals(newText) must beTrue
      }
    }

    "find a RequestAnswer" in {
      running(FakeApplication()) {
        RequestAnswer.find(answer.id) must beSome
      }
    }
    
    "find RequestAnswers by question" in {
      running(FakeApplication()) {
        val ra = answer
        RequestQuestion.find(ra.questionId).get.answers.map(_.id).contains(ra.id) must beTrue
      }
    }
    
  }

}