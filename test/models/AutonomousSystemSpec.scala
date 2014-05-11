package models

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class AutonomousSystemSpec extends Specification {
  
  private val privateAsRangeBegin = 64512
  private val testCountry = "US"
    
  private def testName: String = System.nanoTime.toHexString
  
  private def availableAsn: Int = {
    (privateAsRangeBegin to Integer.MAX_VALUE).foreach { asn =>
    	if (AutonomousSystem.find(asn).isEmpty) {
    	  return asn
    	}
    }
    return privateAsRangeBegin
  }
  
  private def testAs: AutonomousSystem = {
    val asn = availableAsn
    AutonomousSystem.createOrUpdate(List(AsInfo(asn, testName, testCountry)))
    AutonomousSystem.find(asn).get
  }
  
  "AutonomousSystem" should {
    
    "create an AutonomousSystem" in {
      running(FakeApplication()) {
        val infos = List(AsInfo(availableAsn, testName, testCountry), AsInfo(availableAsn+1, testName, testCountry))
        AutonomousSystem.createOrUpdate(infos) must equalTo(infos.size)
      }
    }
    
    "update an AutonomousSystem" in {
      running(FakeApplication()) {
        val as = testAs
        val newName = as.name.reverse + System.nanoTime.toHexString
        val newCountry = as.country.reverse
        as.update(newName, newCountry) must beTrue
        val updated = AutonomousSystem.find(as.number).get
        updated.name.equals(newName) must beTrue
        updated.country.equals(newCountry) must beTrue
      }
    }
    
    "delete an AutonomousSystem" in {
      running(FakeApplication()) {
        val as = testAs
        AutonomousSystem.find(as.number).isDefined must beTrue
        as.delete() must beTrue
        AutonomousSystem.find(as.number).isEmpty must beTrue
      }
    }
    
    "find an AutonomousSystem by ASN" in {
      running(FakeApplication()) {
        val as = testAs
        AutonomousSystem.find(as.number).isDefined must beTrue
      }
    }
    
  }

}