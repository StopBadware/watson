package models

import org.specs2.mutable._
import org.junit.runner._
import org.specs2.runner._
import play.api.test._
import play.api.test.Helpers._

@RunWith(classOf[JUnitRunner])
class HostSpec extends Specification {
  
  "Host" should {
    
    "reverse the labels of a host" in {
      val normal = "www.worldofwarcraft.com"
      val reversed = "com.worldofwarcraft.www"
      Host.reverse(normal) must equalTo (reversed)
      val host = Host(normal)
      host.reversed must equalTo (reversed)
    }
    
  }

}