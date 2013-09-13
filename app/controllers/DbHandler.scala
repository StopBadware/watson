package controllers

import play.api.Logger
import play.api.mvc.Controller

object DbHandler extends Controller {
  
  def importBlacklist(blacklist: Blacklist) {
    println("size: "+blacklist.uris.size,"source: "+blacklist.source,"time: "+blacklist.blTime,"isDiff: "+blacklist.isDifferential) //DELME WTSN-11
    //TODO WTSN-11 add each uri to db
    
    //TODO WTSN-11 ifDff update entries for source not in this list with bltimes < bl.time
  }
  
}

case class Blacklist(uris: Set[String], source: String, blTime: Long, isDifferential: Boolean=true)