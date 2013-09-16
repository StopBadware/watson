package controllers

import java.net.URISyntaxException
import play.api.Logger
import play.api.mvc.Controller
import models._

case class Blacklist(
    uris: Set[String], 
    source: String, 
    blTime: Long, 
    isDifferential: Boolean=true)

object Blacklist extends Controller {
  
  def importBlacklist(blist: Blacklist) {
    println("size: "+blist.uris.size,"source: "+blist.source,"time: "+blist.blTime,"isDiff: "+blist.isDifferential) //DELME WTSN-11
    blist.uris.foreach { rawUri =>
	    try {
	    	val uri = Uri(rawUri)
//	    	addUri(uri)		//DELME?
//	    	uri.blacklist(blist.source, blist.blTime)
	    	Uri(rawUri).blacklist(blist.source, blist.blTime)
    		println(uri.uri,uri.hierarchicalPart,uri.path,uri.query,uri.reversedHost,uri.sha2)	//DELME WTSN-11
	    } catch {
	      case e: URISyntaxException => Logger.warn("Unable to parse a valid URI from "+rawUri+"\t"+e.getMessage)
	    }
    }
    
    if (blist.isDifferential) {
    	//TODO WTSN-11 update entries for source not in blist with bltimes < bl.time
    }
    
  }  

}