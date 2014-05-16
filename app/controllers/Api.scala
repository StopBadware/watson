package controllers

import java.util.UUID
import scala.actors.Futures.future
import scala.util.Try
import scala.io.{Source => IoSource}
import play.api._
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc._
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor
import models._
import models.enums.{ReviewStatus, Source}
import models.cr._

object Api extends Controller with ApiSecured with JsonMapper {
  
  private val numTop = Try(sys.env("NUM_TOP_IP_AS").toInt).getOrElse(50)
  
  def counts = withAuth { implicit request =>
    val byStatus = Review.uniqueUrisByStatus
    val helped = Try(byStatus(ReviewStatus.CLOSED_CLEAN) + byStatus(ReviewStatus.CLOSED_NO_LONGER_REPORTED)).getOrElse(0)
    Ok(Json.obj("current" -> BlacklistEvent.currentUniqueUriCount, "helped" -> helped))
  }
  
	def timeoflast(abbr: String) = withAuth { implicit request =>
		val source = Source.withAbbr(abbr)
		if (source.isDefined) {
		  val blTimeOfLast = BlacklistEvent.timeOfLast(source.get)
		  val redisTimeOfLast = Try(Redis.blacklistTimes(source.get).max).getOrElse(0L)
			Ok(Math.max(blTimeOfLast, redisTimeOfLast).toString)
		} else if (abbr.equalsIgnoreCase("googapl")) {
		  Ok(GoogleRescan.timeOfLast.toString)
		} else {
		  NotFound
		}
  }
	
	def reviewResults(since: Long) = withAuth { implicit request =>
	  val json = ReviewResult.closedSince(since).map { r =>
	    Json.obj(
        "uri" -> r.uri, 
        "opened" -> r.opened, 
        "closed" -> r.closed, 
        "status" -> r.status.toString, 
        "category" -> r.category,
        "bad_code" -> r.badCode,
        "exec_sha2_256" -> r.executableSha256
      )
	  }
    Ok(Json.obj("review_results" -> json))
  }
	
	def communityReports(since: Long) = withAuth { implicit request =>
	  val json = CommunityReport.findSummariesSince(since).map { cr =>
	    Json.obj(
        "uri" -> cr.uri, 
        "description" -> cr.description, 
        "type" -> cr.crType, 
        "source" -> cr.crSource,
        "reported_at" -> cr.reportedAt
      )
	  }
    Ok(Json.obj("community_reports" -> json))
  }
	
	def blacklistedHosts = withAuth { implicit request =>
    Ok(Json.obj("hosts" -> BlacklistEvent.blacklistedHosts))
  }
	
	def topIps = withAuth { implicit request =>
    val topIps = HostIpMapping.top(numTop).map { ip =>
	    Json.obj("ip" -> ip.ip, "asn" -> ip.asNum, "name" -> ip.asName, "num_hosts" -> ip.numHosts, "num_urls" -> ip.numUris)
	  }
	  Ok(Json.obj("as_of" -> HostIpMapping.lastResolvedAt, "top_ip" -> topIps))
  }
	
	def topAsns = withAuth { implicit request =>
	  val topAs = IpAsnMapping.top(numTop).map { as =>
	    Json.obj("number" -> as.asNum, "name" -> as.asName, "num_ips" -> as.numIps, "num_urls" -> as.numUris)
	  }
    Ok(Json.obj("as_of" -> IpAsnMapping.lastMappedAt, "top_as" -> topAs))
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
	  val body = Try(request.body.asJson)
	  if (body.isSuccess) {
	    val json = body.get.get
	    try {
	      Logger.debug("Review requested for '"+json.\("uri")+"'")	//DELME WTSN-50
	      val uri = Uri.findOrCreate(json.\("uri").as[String]).get
	      Logger.debug("uri: "+uri)			//DELME WTSN-50
		    val email = json.\("email").as[String]
	      Logger.debug("email: "+email)	//DELME WTSN-50
		    val ip = Try(Ip.toLong(json.\("ip").as[String]).get).toOption
		    Logger.debug("ip: "+ip)				//DELME WTSN-50
	      val notes = json.\("notes").asOpt[String]
	      Logger.debug("notes:"+notes)	//DELME WTSN-50
	    	if (uri.requestReview(email, ip, notes)) Ok else UnprocessableEntity
	    } catch {
	      case e: Exception => Logger.debug("Review request failed: "+e.getMessage)	//DELME WTSN-50
        BadRequest
	    }
	  } else {
	  	UnsupportedMediaType
	  }
	}
	
	def submitCommunityReport = withAuth { implicit request =>
	  val body = Try(request.body.asJson)
	  if (body.isSuccess) {
	    val created = Application.createCommunityReports(body.get.get)
	    if (created > 0) Ok(created.toString) else BadRequest
	  } else {
	  	UnsupportedMediaType
	  }
	}
	
}

trait ApiSecured {
  
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
	
	private def onUnauthorized(request: RequestHeader) = play.api.mvc.Results.Forbidden  
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
