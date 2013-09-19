package models

import java.net.{URI, URISyntaxException}
import controllers.{DbHandler => dbh, Hash}
import com.mongodb.casbah.Imports._

/**
 * Represents a MongoDB document from the uris collection
 */
protected class Uri(uriDoc: DBObject) extends MongoDoc(uriDoc) {
  
  val uri = uriDoc.getAsOrElse[String]("uri", "")
  val path = uriDoc.getAsOrElse[String]("path", "")
  val query = uriDoc.getAsOrElse[String]("query", "")
  val hierPart = uriDoc.getAsOrElse[String]("hierPart", "")
  val reversedHost = uriDoc.getAsOrElse[String]("reversedHost", "")
  val sha256 = uriDoc.getAsOrElse[String]("sha256", "")
//  val blacklistEvents = uriDoc.getAs[MongoDBList]("zblacklistEvents").map {// event =>
//    val event = new BlacklistEvent(_)
//    event
//  }
//  println(blacklistEvents,blacklistEvents.getClass)
//  val foo = blacklistEvents.map(_.asInstanceOf[BasicDBObject])
//  println(foo,foo.getClass)
//  val delme = foo.map { f => 
//    val bar = new BlacklistEvent(f)
//    println(bar.by,bar.from,bar.to)
//    bar.to
//  }
//  println(delme)
  def isBlacklisted: Boolean = true		//TODO WTSN-11
  override def toString: String = uri 
  
  class BlacklistEvent(doc: BasicDBObject) {
    val by = doc.getAsOrElse[String]("by", "")
    val from = doc.getAsOrElse[Long]("from", 0)
//    val from = doc.getAsOrElse[Option[Long]]("from", None)
    val to: Option[Long] = doc.getAs[Long]("to")
  }
  
}

object Uri {
  
  def apply(uriDoc: DBObject): Uri = new Uri(uriDoc)
  
}

/**
 * Raw reported/blacklisted URI
 * @param uriStr - if the passed string does not have a scheme 'http' will be assumed
 * by prefixing the passed string with 'http://'
 * @throws URISyntaxException if a valid URI (as specified by RFC-2396) cannot be parsed
 */
@throws[URISyntaxException]
class ReportedUri(uriStr: String) {
  
  val uri: URI = {
    val schemeCheck = "^[a-zA-Z]+[a-zA-Z0-9+.\\-]+://.*"
    val withScheme = if (uriStr.matches(schemeCheck)) uriStr else "http://" + uriStr
    new URI(withScheme)
  }
  val path = uri.getRawPath
  val query = uri.getRawQuery
  val hierarchicalPart = uri.getRawAuthority + uri.getRawPath
  lazy val reversedHost = Host.reverse(uri.getHost)
  lazy val sha256 = Hash.sha256(uri.toString)
  
  override def hashCode: Int = uri.hashCode
  override def toString: String = uri.toString 
  def blacklist(source: String, time: Long) = dbh.blacklist(ReportedUri.this, source, time)
  def removeFromBlacklist(source: String, time: Long) = dbh.removeFromBlacklist(ReportedUri.this, source, time)

}