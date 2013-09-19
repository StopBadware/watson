package models

import com.mongodb.casbah.Imports._

/**
 * Parent class for all models mapped to MongoDB documents
 */
class MongoDoc(doc: DBObject) {
  
  val oid = doc.get("_id").asInstanceOf[ObjectId]
  val id = oid.toString
  val createdAt = oid._time
  
  def compareTo(that: MongoDoc) = oid.compareTo(that.oid)
  override def hashCode: Int = oid.hashCode
  override def toString: String = id

}