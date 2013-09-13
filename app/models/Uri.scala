package models

import java.net.URI

case class Uri(id: Int, uri: URI)

object Uri {
  def blacklisted(uri: String, source: String, blacklistedTime: Long) {
    //TODO WTSN-11 store entry in db
    println("ADDTOBL:"+uri+"\t"+source+"\t"+blacklistedTime)	//DELME
  }
  
  def removeFromBlacklist(uri: String, source: String, removedTime: Long) {
    //TODO WTSN-11 update entry in db
    println("UNBL:"+uri+"\t"+source+"\t"+removedTime)	//DELME
  }  
}