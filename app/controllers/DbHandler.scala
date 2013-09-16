package controllers

import java.math.BigInteger
import java.net.URI
import java.security.MessageDigest
import play.api.Logger
import play.api.mvc.Controller
import com.mongodb.casbah.Imports._
import models._

object DbHandler extends Controller {
  
  val mongoUrl = MongoClientURI(sys.env("MONGO_URL"))
  val db = MongoClient(mongoUrl).getDB(mongoUrl.database.get)
  def sha2 = Hash.sha2(_)
  
  def importBlacklist(blist: Blacklist) {
    println("size: "+blist.uris.size,"source: "+blist.source,"time: "+blist.blTime,"isDiff: "+blist.isDifferential) //DELME WTSN-11
    blist.uris.foreach(upsertUri(_, blist.source, blist.blTime))
    //DELME
    println(db.collectionNames)
    //DELME
    if (blist.isDifferential) {
    	//TODO WTSN-11 update entries for source not in blist with bltimes < bl.time
    }
    
  }
  
  private def upsertUri(uri: String, source: String, blTime: Long) {
  	//TODO WTSN-11 add/update uri in db
//    println(uri, sha2(uri), source, blTime)	//DELME WTSN-11 //DELME WTSN-11
    val foo = Uri(uri)	 //DELME
    println(foo.uri,foo.hierarchicalPart,foo.path,foo.query,foo.reversedHost,foo.sha2)	//DELME WTSN-11
    //TODO WTSN-11 create new URI()
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

case class Blacklist(uris: Set[String], source: String, blTime: Long, isDifferential: Boolean=true)