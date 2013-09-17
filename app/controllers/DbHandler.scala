package controllers

import java.math.BigInteger
import java.security.MessageDigest
import play.api.Logger
import play.api.mvc.Controller
import com.mongodb.casbah.Imports._
import models._

object DbHandler extends Controller {
  
  private val mongoUrl = MongoClientURI(sys.env("MONGO_URL"))
  private val db = MongoClient(mongoUrl).getDB(mongoUrl.database.get)
  private val autonomousSystems = db("autonomousSystems")
  private val hosts = db("hosts")
  private val ips = db("ips")
  private val uris = db("uris")
  private val DupeErr = 11000
  
  def blacklist(uri: Uri, source: String, time: Long) {
    val uriDoc = MongoDBObject(
        "uri" -> uri.toString,
        "path" -> uri.path,
        "query" -> uri.query,
        "hierPart" -> uri.hierarchicalPart,
        "reversedHost" -> uri.reversedHost,
        "sha256" -> uri.sha256)
    val eventDoc = {
      $addToSet("blacklistEvents" -> 
      	MongoDBObject("by" -> source, "from" -> time, "to" -> 0L)) ++ 
      $set("blacklisted" -> true)
    }
    
    try {
    	uris.update(uriDoc, eventDoc, true, false)
    } catch {
      case e: MongoException => Logger.error("Upsert failed: " + e.getMessage)
    }
  }
  
  def removeFromBlacklist(uri: Uri, source: String, time: Long) {
    //TODO WTSN-11 change blacklisted flag if not blacklisted by any other source
    //TODO WTSN-11 add cleantime for this source 
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