package controllers

import java.util.UUID
import scala.actors.Futures.future
import scala.util.Try
import scala.io.{Source => IoSource}
import play.api._
import play.api.mvc._
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor
import models.{BlacklistEvent, Uri}
import models.enums.Source

object Api extends Controller with JsonMapper {
  
	def timeoflast(abbr: String) = withAuth { implicit request =>
		val source = Source.withAbbr(abbr)
		if (source.isDefined) {
		  val blTimeOfLast = BlacklistEvent.timeOfLast(source.get)
		  val redisTimeOfLast = Try(Redis.blacklistTimes(source.get).max).getOrElse(0L)
			Ok(Math.max(blTimeOfLast, redisTimeOfLast).toString)
		} else {
		  NotFound
		}
  }
	
	def importList(abbr: String) = withAuth { implicit request =>
	  Logger.info("Received import for " + abbr)
	  val source = Source.withAbbr(abbr)
	  val json = request.body.asJson.mkString
	  if (source.isDefined) {
	    future(Blacklist.importBlacklist(json, source.get))
	    Ok
	  } else if (abbr.equalsIgnoreCase("googapl")) {
	    future(Blacklist.importGoogleAppeals(json))
	    Ok
	  } else {
	  	NotFound
	  }
  }
	
	def requestReview = withAuth { implicit request =>
	  val body = Try(mapJson(request.body.asJson.get.toString).get)
	  if (body.isSuccess) {
	    val json = body.get
	    try {
		    val uri = Uri.findOrCreate(json.get("uri").asText).get
		    val email = json.get("email").asText
		    val ip = if (json.has("ip")) Some(json.get("ip").asLong) else None
		    val notes = if (json.has("notes")) Some(json.get("notes").asText) else None
	    	if (uri.requestReview(email, ip, notes)) Ok else UnprocessableEntity
	    } catch {
	      case _: Exception => BadRequest
	    }
	  } else {
	  	UnsupportedMediaType
	  }
	}
	
	def withAuth(auth: => Request[AnyContent] => Result) = {
    Security.Authenticated(apiKey, onUnauthorized) { implicit request =>
      Action(request => auth((request)))
    }
  }
	
	private def apiKey(request: RequestHeader): Option[String] = {
	  return try {
	    val headers = request.headers
	    val key = headers("Wtsn-Key")
	    if (ApiAuth.authenticate(key, headers("Wtsn-Timestamp").toLong, request.path, headers("Wtsn-Signature"))) {
	      Some(key)
	    } else {
	      None
	    }
	  } catch {
	    case _: Exception => None
	  }
	}
	
	private def onUnauthorized(request: RequestHeader) = Forbidden
	
}

object ApiAuth {
  
  private val cryptAlg = sys.env("CRYPT_ALG")
  private val cryptKey = sys.env("CRYPT_KEY")
  private val maxAge = Try(sys.env("API_MAX_SECONDS").toInt).getOrElse(60)
  private val textEncryptor = new StandardPBEStringEncryptor()
  
  textEncryptor.setAlgorithm(cryptAlg)
  textEncryptor.setPassword(cryptKey)
  
  def newPair: Option[(String, String)] = {
  	val pubKey = UUID.randomUUID + "-" + System.currentTimeMillis.toHexString
  	val secret = Hash.sha256(UUID.randomUUID + System.nanoTime.toHexString).get
  	val crypted = encrypt(secret)
  	return if (crypted.isDefined) {
  		Redis.set(pubKey, crypted.get)
  		Some((pubKey, secret))
  	} else {
  	  None
  	}
  }
  
  def dropPair(pubKey: String): Boolean = Redis.drop(pubKey)
  
  def authenticate(pubKey: String, timestamp: Long, path: String, signature: String): Boolean = {
    val cryptedSecret = Redis.get(pubKey)
    val authenticated = if (cryptedSecret.isDefined && validateTimestamp(timestamp)) {
      Try(Hash.sha256(pubKey + timestamp + path + decrypt(cryptedSecret.get).getOrElse("")).get.equals(signature)).getOrElse(false)
    } else {
      false
    }
    if (authenticated) {
      Logger.info(pubKey+" accessing "+path)
    } else {
      Logger.warn("Authentication FAILURE for "+pubKey+" accessing "+path)
    }
    return authenticated
  }
  
  private def validateTimestamp(timestamp: Long): Boolean = {
    val now = System.currentTimeMillis / 1000
    return timestamp < (now + 10) && (now - timestamp) < maxAge
  }
  
  private def encrypt(cleartext: String): Option[String] = Try(textEncryptor.encrypt(cleartext)).toOption
  
  private def decrypt(ciphertext: String): Option[String] = Try(textEncryptor.decrypt(ciphertext)).toOption
  
}