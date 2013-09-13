package controllers

import play.api.Logger
import play.api.mvc.Controller
import com.mongodb.casbah.Imports._

object DbHandler extends Controller {
  
  val mongoUrl = MongoClientURI(sys.env("MONGO_URL"))
  val db = MongoClient(mongoUrl).getDB(mongoUrl.database.get)
  
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
    println(uri, source, blTime)	//DELME WTSN-11
  }
  
}

case class Blacklist(uris: Set[String], source: String, blTime: Long, isDifferential: Boolean=true)