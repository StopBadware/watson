package controllers

import java.math.BigInteger
import java.security.MessageDigest
import play.api.Logger
import play.api.mvc.Controller
import models._

object DbHandler extends Controller {
  
  def blacklist(reported: String, time: Long, source: String) = {
    println("*****")	//DELME WTSN-11
    println(reported, time, source)	//DELME WTSN-11
//    val uri = findOrCreate(reported)
//    println(uri.size,uri.isDefined,uri.getClass)	//DELME WTSN-11
//    uri.foreach { u =>
//    	val bar = Uri
//    	println(bar.hierPart,bar.id,bar.createdAt,bar.path,bar.query,bar.reversedHost,bar.sha256,bar.uri)
//    }
    //TODO WTSN-11 see if already blacklisted by this source in blacklist_events
    
    //TODO WTSN-11 update blacklist_events IF new fromtime is older OR not blisted by source
    
    println("*****")	//DELME WTSN-11
  }
  
  def removeFromBlacklist(uri: ReportedUri, source: String, time: Long) = {
    //TODO WTSN-11 change blacklisted flag if not blacklisted by any other source
    //TODO WTSN-11 add cleantime for this source 
  }
  
  def findOrCreate(uri: ReportedUri): Option[Uri] = {
    return None	//TODO WTSN-11
  }
  
}

object Hash {
  def sha256(msg: String): Option[String] = {
    return try {
      val md = MessageDigest.getInstance("SHA-256").digest(msg.getBytes)
      val sha2 = (new BigInteger(1, md)).toString(16)
      Some(sha2.format("%64s", sha2).replace(' ', '0'))
    } catch {
      case e: Exception => None
    }
  }
}