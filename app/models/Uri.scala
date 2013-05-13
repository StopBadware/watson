package models

import java.net.URI

case class Uri(id: Int, uri: URI)

object Uri {
  def add(uri: String, source: String, blacklistedTime: Long) = {
    //TODO: store entries in db
    println("TOWRITE:"+uri+"\t"+source+"\t"+blacklistedTime)	//DELME
  }
}