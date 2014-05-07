package models

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._
import controllers.Ip

@RunWith(classOf[JUnitRunner])
class IpAsnMappingSpec extends Specification {
  
  private val privateAsRangeBegin = 64512
  private val privateIpRangeBegin = Ip.toLong("10.0.0.0").get
  private val privateIpRangeEnd = Ip.toLong("10.255.255.255").get
  
  private def testIp: Long = {
    (privateIpRangeBegin to privateIpRangeEnd).foreach { ip =>
    	if (IpAsnMapping.findByIp(ip).isEmpty) {
    	  AutonomousSystem.createOrUpdate(privateAsRangeBegin, System.currentTimeMillis.toHexString, "US")
    	  IpAsnMapping.create(ip, privateAsRangeBegin)
    	  return ip
    	}
    }
    return privateIpRangeBegin
  }
  
  private def nextAsn(ip: Long): Int = {
    val asns = IpAsnMapping.findByIp(ip).map(_.asn)
  	val newAsn = if (asns.isEmpty) privateAsRangeBegin else asns.max + 1
    AutonomousSystem.createOrUpdate(newAsn, System.currentTimeMillis.toHexString, "US")
    return newAsn
  }
  
  "IpAsnMapping" should {
    
    "create an IpAsnMapping" in {
      running(FakeApplication()) {
        val ip = testIp
        IpAsnMapping.create(ip, nextAsn(ip)) must beTrue
      }
    }
    
    "delete an IpAsnMapping" in {
      running(FakeApplication()) {
        val ip = testIp
        val asn = nextAsn(ip)
        IpAsnMapping.create(ip, asn)
        val found = IpAsnMapping.findByIp(ip) 
        found.map(_.asn).contains(asn) must beTrue
        found.filter(_.asn == asn).map(_.delete() must beTrue)
        IpAsnMapping.findByIp(ip).map(_.asn).contains(asn) must beFalse
      }
    }
    
    "find an IpAsnMapping" in {
      running(FakeApplication()) {
        val ip = testIp
        val asn = nextAsn(ip)
        IpAsnMapping.create(ip, asn)
        IpAsnMapping.find(IpAsnMapping.findByIp(ip).head.id) must beSome
      }
    }
    
    "find IpAsnMappings by IP" in {
      running(FakeApplication()) {
        val ip = testIp
        val asn = nextAsn(ip)
        IpAsnMapping.create(ip, asn)
        IpAsnMapping.findByIp(ip).map(_.asn).contains(asn) must beTrue
      }
    }
    
    "find IpAsnMappings by ASN" in {
      running(FakeApplication()) {
        val ip = testIp
        val asn = nextAsn(ip)
        IpAsnMapping.create(ip, asn)
        IpAsnMapping.findByAsn(asn).map(_.ip).contains(ip) must beTrue
      }
    }
    
  }  

}