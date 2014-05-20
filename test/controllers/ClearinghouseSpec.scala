package controllers

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._
import models.{BlacklistEvent, HostIpMapping, IpAsnMapping, Uri}
import models.enums.Source

@RunWith(classOf[JUnitRunner])
class ClearinghouseSpec extends Specification {
  
	private val source = Source.GOOG
	private val privateAsRangeBegin = 64512
  private val privateIpRangeBegin = Ip.toLong("10.0.0.0").get
  private def validUri(base: String): Uri = Uri.findOrCreate(base+".com/" + System.nanoTime.toHexString).get
  
  "Clearinghouse" should {
    
    "find all URIs for a host" in {
      running(FakeApplication()) {
        val time = (System.currentTimeMillis / 1000) - 1000
        val base = "chspec" + System.nanoTime.toHexString
        val uriA = validUri(base)
        val uriB = validUri(base)
        val uriC = validUri("www." + base)
        val uriD = validUri("blog." + base)
        val uriE = validUri("sub.www." + base)
        uriA.blacklist(source, time)
        uriB.blacklist(source, time, Some(System.currentTimeMillis / 1000))
        uriC.blacklist(source, time)
        uriD.blacklist(source, time)
        uriE.blacklist(source, time)
        val uriIds = List(uriA.id, uriB.id, uriC.id, uriD.id, uriE.id)
        
        val s1UriIds = Clearinghouse.findUrisWithSiblingsAndChildren(uriA.host, false).map(_.uriId)
        uriIds.map(s1UriIds.contains(_) must beTrue)
        
        val s2UriIds = Clearinghouse.findUrisWithSiblingsAndChildren(uriC.host, false).map(_.uriId)
        uriIds.map(s2UriIds.contains(_) must beTrue)
        
        val s3UriIds = Clearinghouse.findUrisWithSiblingsAndChildren(uriE.host, false).map(_.uriId)
        s3UriIds.contains(uriA.id) must beFalse
        s3UriIds.contains(uriD.id) must beFalse
        s3UriIds.contains(uriC.id) must beTrue
        s3UriIds.contains(uriE.id) must beTrue
      }
    }
    
    "find all currently blacklisted URIs for a host" in {
      running(FakeApplication()) {
        val time = (System.currentTimeMillis / 1000) - 1000
        val base = "chspec" + System.nanoTime.toHexString
        val uriA = validUri(base)
        val uriB = validUri(base)
        val uriC = validUri("www." + base)
        uriA.blacklist(source, time)
        uriB.blacklist(source, time, Some(System.currentTimeMillis / 1000))
        uriC.blacklist(source, time)
        val uriIds = List(uriA.id, uriB.id, uriC.id)
        
        val searchUriIds = Clearinghouse.findUrisWithSiblingsAndChildren(uriA.host, true).map(_.uriId)
        searchUriIds.contains(uriA.id) must beTrue
        searchUriIds.contains(uriB.id) must beFalse
        searchUriIds.contains(uriC.id) must beTrue
      }
    }
    
    "find blacklisted URI count and AS info for an IP" in {
      running(FakeApplication()) {
        val uri = validUri("chspec" + System.nanoTime.toHexString)
        val ip = privateIpRangeBegin
        val asn = privateAsRangeBegin
        val now = System.currentTimeMillis / 1000
        BlacklistEvent.create(List(uri.id), Source.GOOG, now, None)
        HostIpMapping.createOrUpdate(Map(uri.reversedHost -> ip), now)
        IpAsnMapping.createOrUpdate(Map(ip -> asn), now)
        val chIp = Clearinghouse.ipSearch(ip)
        chIp.asNum must equalTo(Some(asn))
        chIp.numBlacklistedUris must be_>(0)
      }
    }
    
    "find blacklisted URI and IP counts for an Autonomous System" in {
      running(FakeApplication()) {
        val uriA = validUri("a" + System.nanoTime.toHexString)
        val uriB = validUri("b" + System.nanoTime.toHexString)
        val ipA = privateIpRangeBegin
        val ipB = privateIpRangeBegin + 1
        val asn = privateAsRangeBegin
        val now = System.currentTimeMillis / 1000
        BlacklistEvent.create(List(uriA.id, uriB.id), Source.GOOG, now, None)
        HostIpMapping.createOrUpdate(Map(uriA.reversedHost -> ipA, uriB.reversedHost -> ipB), now)
        IpAsnMapping.createOrUpdate(Map(ipA -> asn, ipB -> asn), now)
        val blacklistedIps = Clearinghouse.blacklistedIps(asn)
        blacklistedIps.size must be_>=(2)
        blacklistedIps.contains(ipA) must beTrue
        blacklistedIps.contains(ipB) must beTrue
        Clearinghouse.blacklistedUrisCount(blacklistedIps) must be_>=(2)
      }
    }
    
  }

}