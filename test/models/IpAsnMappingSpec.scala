package models

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._
import controllers.{Host, Ip}

@RunWith(classOf[JUnitRunner])
class IpAsnMappingSpec extends Specification {
  
  private val privateAsRangeBegin = 64512
  private val privateIpRangeBegin = Ip.toLong("10.0.0.0").get
  private val privateIpRangeEnd = Ip.toLong("10.255.255.255").get
  private val asOf = System.currentTimeMillis / 1000
  
  private def testIp: Long = {
    (privateIpRangeBegin to privateIpRangeEnd).foreach { ip =>
    	if (IpAsnMapping.findByIp(ip).isEmpty) {
    	  AutonomousSystem.createOrUpdate(List(AsInfo(privateAsRangeBegin, System.currentTimeMillis.toHexString, "US")))
    	  IpAsnMapping.createOrUpdate(Map(ip -> privateAsRangeBegin), asOf)
    	  return ip
    	}
    }
    return privateIpRangeBegin
  }
  
  private def nextAsn(ip: Long): Int = {
    val asns = IpAsnMapping.findByIp(ip).map(_.asn)
  	val newAsn = if (asns.isEmpty) privateAsRangeBegin else asns.max + 1
    AutonomousSystem.createOrUpdate(List(AsInfo(newAsn, System.currentTimeMillis.toHexString, "US")))
    return newAsn
  }
  
  "IpAsnMapping" should {
    
    "create an IpAsnMapping" in {
      running(FakeApplication()) {
        val ip = testIp
        val mappings = Map(ip -> nextAsn(ip), privateIpRangeEnd -> privateAsRangeBegin)
        IpAsnMapping.createOrUpdate(mappings, asOf) must equalTo(mappings.size)
      }
    }
    
    "delete an IpAsnMapping" in {
      running(FakeApplication()) {
        val ip = testIp
        val asn = nextAsn(ip)
        IpAsnMapping.createOrUpdate(Map(ip -> asn), asOf)
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
        IpAsnMapping.createOrUpdate(Map(ip -> asn), asOf)
        IpAsnMapping.find(IpAsnMapping.findByIp(ip).head.id) must beSome
      }
    }
    
    "find IpAsnMappings by IP" in {
      running(FakeApplication()) {
        val ip = testIp
        val asn = nextAsn(ip)
        IpAsnMapping.createOrUpdate(Map(ip -> asn), asOf)
        IpAsnMapping.findByIp(ip).map(_.asn).contains(asn) must beTrue
      }
    }
    
    "find IpAsnMappings by ASN" in {
      running(FakeApplication()) {
        val ip = testIp
        val asn = nextAsn(ip)
        IpAsnMapping.createOrUpdate(Map(ip -> asn), asOf)
        IpAsnMapping.findByAsn(asn).map(_.ip).contains(ip) must beTrue
      }
    }
    
    "get last mapped time" in {
      running(FakeApplication()) {
        val ip = testIp
        val asn = nextAsn(ip)
        IpAsnMapping.createOrUpdate(Map(ip -> asn), asOf)
        IpAsnMapping.lastMappedAt must be_>=(asOf)
      }
    }
    
    "get autonomous systems with most URIs blacklisted" in {
      running(FakeApplication()) {
      	val host = "example" + System.currentTimeMillis + ".com"
        val ip = testIp
        val asn = nextAsn(ip)
        Uri.create(host)
        HostIpMapping.createOrUpdate(Map(Host.reverse(host) -> ip), asOf)
        IpAsnMapping.createOrUpdate(Map(ip -> privateAsRangeBegin), asOf)
        IpAsnMapping.top(5).nonEmpty must beTrue
      }
    }
    
  }  

}