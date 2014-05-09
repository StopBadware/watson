package models

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._
import controllers.{Host, Ip}

@RunWith(classOf[JUnitRunner])
class HostIpMappingSpec extends Specification {
  
  private val privateIpRangeBegin = Ip.toLong("10.0.0.0").get
  private val privateAsRangeBegin = 64512
  private val asOf = System.currentTimeMillis / 1000
  
  private def testReversedHost: String = "com." + System.nanoTime.toHexString + ".www" 
  
  private def nextIp(host: String): Long = {
    val ips = HostIpMapping.findByHost(host).map(_.ip)
    return if (ips.isEmpty) privateIpRangeBegin else ips.max + 1
  }
    
  "HostIpMapping" should {
    
    "create a HostIpMapping" in {
      running(FakeApplication()) {
        val host = testReversedHost
        HostIpMapping.create(host, nextIp(host), asOf) must beTrue
      }
    }
    
    "delete a HostIpMapping" in {
      running(FakeApplication()) {
        val host = testReversedHost
        val ip = nextIp(host)
        HostIpMapping.create(host, ip, asOf)
        val found = HostIpMapping.findByHost(host)
        found.nonEmpty must beTrue
        found.map(_.ip).contains(ip) must beTrue
        found.filter(_.ip == ip).map(_.delete() must beTrue)
        HostIpMapping.findByHost(host).map(_.ip).contains(ip) must beFalse
      }
    }
    
    "find a HostIpMapping" in {
      running(FakeApplication()) {
        val host = testReversedHost
        HostIpMapping.create(host, nextIp(host), asOf)
        HostIpMapping.find(HostIpMapping.findByHost(host).head.id) must beSome
      }
    }
    
    "find HostIpMappings by IP" in {
      running(FakeApplication()) {
        val host = testReversedHost
        val ip = nextIp(host)
        HostIpMapping.create(host, ip, asOf)
        HostIpMapping.findByIp(ip).map(_.reversedHost).contains(host) must beTrue
      }
    }
    
    "find HostIpMappings by host" in {
      running(FakeApplication()) {
        val host = testReversedHost
        val ip = nextIp(host)
        HostIpMapping.create(host, ip, asOf)
        HostIpMapping.findByHost(host).map(_.ip).contains(ip) must beTrue
      }
    }
    
    "get last resolved at time" in {
      running(FakeApplication()) {
        val host = testReversedHost
        HostIpMapping.create(host, nextIp(host), asOf)
        HostIpMapping.lastResolvedAt must be_>=(asOf)
      }
    }
    
    "get IPs with most URIs blacklisted" in {
      running(FakeApplication()) {
        val host = testReversedHost
        val ip = nextIp(host)
        Uri.create(Host.reverse(host))
        HostIpMapping.create(host, ip, asOf)
        IpAsnMapping.create(ip, privateAsRangeBegin, asOf)
        HostIpMapping.top(5).nonEmpty must beTrue
      }
    }
    
  }  

}