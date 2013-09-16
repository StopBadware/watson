package controllers

import java.math.BigInteger
import java.security.MessageDigest
import play.api.Logger
import play.api.mvc.Controller
import com.mongodb.casbah.Imports._
import models._

object DbHandler extends Controller {
  
  val mongoUrl = MongoClientURI(sys.env("MONGO_URL"))
  val db = MongoClient(mongoUrl).getDB(mongoUrl.database.get)
  
//  private def addUri(uri: Uri) {	//DELME WTSN-11 ???
//  	//TODO WTSN-11 add uri in db
//  }
  
  def blacklist(uri: Uri, source: String, time: Long) {
    //TODO WTSN-11 change blacklisted flag if not already blacklisted by any source
    //TODO WTSN-11 add source/time entry if not already blacklisted by this source
  }
  
  def removeFromBlacklist(uri: Uri, source: String, time: Long) {
    //TODO WTSN-11 change blacklisted flag if not blacklisted by any other source
    //TODO WTSN-11 add cleantime for this source 
  }
  
}

object Hash {
  def sha2(msg: String): Option[String] = {
    return try {
      val md = MessageDigest.getInstance("SHA-256").digest(msg.getBytes)
      val sha2 = (new BigInteger(1, md)).toString(16)
      Some(sha2.format("%64s", sha2).replace(' ', '0'))
    } catch {
      case e: Exception => None
    }
  }
}