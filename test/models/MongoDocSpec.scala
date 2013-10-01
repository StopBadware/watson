package models

import org.specs2.mutable._

import play.api.test._
import play.api.test.Helpers._
import org.bson.types.ObjectId
import com.mongodb.casbah.Imports._

class MongoDocSpec extends Specification {
  
  "a MongoDoc" should {
    
    "compare two MongoDocs" in {
      val doc = new MongoDoc(MongoDBObject("_id" -> ObjectId.get()))
      val another = new MongoDoc(MongoDBObject("_id" -> ObjectId.get()))
      doc.compareTo(another) must be_!=(0)
      doc.compareTo(doc) must be_==(0)
    }
    
  }

}