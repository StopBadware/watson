package controllers

import java.math.BigInteger
import java.security.MessageDigest
import play.api.Logger
import play.api.mvc.Controller
import com.mongodb.casbah.Imports._
import models._
import com.mongodb.BasicDBObject

object DbHandler extends Controller {
  
  private val mongoUrl = MongoClientURI(sys.env("MONGO_URL"))
  private val db = MongoClient(mongoUrl).getDB(mongoUrl.database.get)
  private val autonomousSystems = db("autonomousSystems")
  private val hosts = db("hosts")
  private val ips = db("ips")
  private val uris = db("uris")
  private val DupeErr = 11000
  
  def blacklist(reported: ReportedUri, source: String, time: Long) {
    println("*****")	//DELME WTSN-11
    val uri = findOrCreate(reported)
    println(uri.size,uri.isDefined,uri.getClass)	//DELME WTSN-11
    uri.foreach { u =>
    	val bar = Uri(u)
    	println(bar.hierPart,bar.id,bar.createdAt,bar.path,bar.query,bar.reversedHost,bar.sha256,bar.uri)
    
    }
    //TODO WTSN-11 see if already blacklisted by this source
    
    //TODO WTSN-11 update IF new fromtime is older OR not blisted by source
    val eventDoc = {
      $addToSet("blacklistEvents" -> 
      	MongoDBObject("by" -> source, "from" -> time, "to" -> None)) 
    }
    
    try {
    	uris.update(uri.get, eventDoc, true, false)
    } catch {
      case e: MongoException => Logger.error("Upsert failed: " + e.getMessage)
    }
    println("*****")	//DELME WTSN-11
  }
  
  def removeFromBlacklist(uri: ReportedUri, source: String, time: Long) {
    //TODO WTSN-11 change blacklisted flag if not blacklisted by any other source
    //TODO WTSN-11 add cleantime for this source 
  }
  
  def findOrCreate(uri: ReportedUri): Option[DBObject] = {
    val found = uris.findOne(MongoDBObject("sha256" -> uri.sha256))
    val foundOrCreated = if (found.isDefined) {
      found
    } else {
      val uriDoc = MongoDBObject(
        "uri" -> uri.toString,
        "path" -> uri.path,
        "query" -> uri.query,
        "hierPart" -> uri.hierarchicalPart,
        "reversedHost" -> uri.reversedHost,
        "sha256" -> uri.sha256)
      uris.save(uriDoc)
      uris.findOne(MongoDBObject("sha256" -> uri.sha256))
    }
    
    return foundOrCreated.map(_.asDBObject)
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