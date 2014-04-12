package controllers

import java.util.UUID
import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.cache.Cache
import play.api.Play.current
import scala.util.Try
import routes.javascript._
import models._
import models.cr._
import models.enums._

object Application extends Controller with Secured with Cookies {
  
  private val limit = Try(sys.env("FILTER_LIMIT").toInt).getOrElse(500)
  
  def index = withAuth { userId => implicit request =>
    Ok(views.html.index("Watson"))
  }
  
  def reviews = withAuth { userId => implicit request =>
    val summaries = Review.summaries(new ReviewSummaryParams(
      request.getQueryString("status"),
      request.getQueryString("blacklisted"),
      request.getQueryString("created")
    ), limit)
    Ok(views.html.reviews(summaries, limit))
    	.withCookies(cookies(request, List("status", "blacklisted", "created")):_*)
  }
  
  def review(id: Int) = withAuth { userId => implicit request =>
    val review = Review.find(id)
    if (review.isDefined) {
      Ok(views.html.review(review.get.details, User.find(userId.get).get))
    } else {
    	Ok(views.html.partials.modelnotfound("Review "+id))
    }
  }
  
  def updateReviewStatus = withAuth { userId => implicit request =>
    val json = request.body.asJson
    val user = User.find(userId.get)
    val status = Try(ReviewStatus.fromStr(json.get.\("status").as[String].replaceAll("-", "_")).get).toOption
    val id = json.get.\("id").asOpt[Int]
    if (json.isDefined && user.isDefined && id.isDefined && status.isDefined) {
    	val review = Review.find(id.get)
    	if (review.isDefined) {
    	  val r = review.get
    	  val s = status.get
    	  val u = user.get.id
  			val updated = s match {
    	    case ReviewStatus.PENDING_BAD => r.reviewed(u, s) 
    	    case ReviewStatus.CLOSED_CLEAN => r.reviewed(u, s)
		      case ReviewStatus.CLOSED_WITHOUT_REVIEW => r.closeWithoutReview(u)
		      case ReviewStatus.CLOSED_BAD => r.verify(u, s)
		      case ReviewStatus.REJECTED => r.reject(u)
		      case ReviewStatus.REOPENED => r.reopen(u)
		      case _ => false
			}
    	  if (updated) {
    	    val rev = Review.find(id.get).get
    	    Ok(Json.obj(
    	        "status" -> rev.status.toString, 
    	        "is_open" -> rev.status.isOpen,
    	        "updated_at" -> rev.statusUpdatedAt)) 
    	  } else {
    	    InternalServerError
    	  }
    	} else {
    		BadRequest(Json.obj("msg" -> "Review Not Found"))
    	}
    } else {
      BadRequest
    }
  }
  
  def updateReviewTestData = withAuth { userId => implicit request =>
    val json = request.body.asJson
    val user = User.find(userId.get)
    if (json.isDefined && user.isDefined) {
      try {
        val id = json.get.\("id").as[Int]
        val review = Review.find(id).get
        
        review.addTag(json.get.\("category").as[String])
        
        val associated = json.get.\("associated_uris").as[Array[JsValue]]
        val incoming = associated.map { au =>
          val uri = au.\("uri").as[String]
          (uri, Uri.findOrCreate(uri).get.id)
        }.toMap
        val uris = incoming.values.toList
        AssociatedUri.findByReviewId(review.id).filterNot(au => uris.contains(au.uriId)).foreach(_.delete())
        
        associated.map { au =>
          val uriId = incoming(au.\("uri").as[String])
          val resolved = au.\("resolved").as[String].toUpperCase match {
            case "RESOLVED" => Some(true)
            case "DNR" => Some(false)
            case _ => None
          }
          RevAssocUri(uriId, resolved, au.\("type").asOpt[String], au.\("intent").asOpt[String])
        }.foreach { au => 
          AssociatedUri.create(review.id, au.uriId, au.resolved, UriType.fromStr(au.uriType.get), UriIntent.fromStr(au.intent.get))
        }
        
        val sha256 = json.get.\("sha256").as[String]
        val badCode = json.get.\("bad_code").as[String]
        ReviewCode.createOrUpdate(
          review.id,
          if (badCode.nonEmpty) Some(badCode) else None,
          if (sha256.length == 64) Some(sha256) else None)
        
        if (json.get.\("mark_bad").as[Boolean]) {
          val updated = review.reviewed(user.get.id, ReviewStatus.PENDING_BAD)
          if (updated) {
            val rev = Review.find(id).get
        		Ok(Json.obj(
    	        "status" -> rev.status.toString, 
    	        "is_open" -> rev.status.isOpen,
    	        "updated_at" -> rev.statusUpdatedAt))
          } else {
            InternalServerError
          }
        } else {
          Ok
        }
      } catch {
        case _: Exception => BadRequest
      }
    } else {
      BadRequest
    }
  }
  
  def addReviewNote = withAuth { userId => implicit request =>
    val json = request.body.asJson
    val user = User.find(userId.get)
    val id = json.get.\("id").asOpt[Int]
    val note = json.get.\("note").asOpt[String]
    if (json.isDefined && user.isDefined && id.isDefined && note.isDefined) {
      val created = ReviewNote.create(id.get, user.get.id, note.get)
      if (created) {
        val notes = Review.find(id.get).get.notes.map { n =>
          Json.obj("id" -> n.id,
            "author" -> n.author,
            "note" -> n.note,
            "created_at" -> n.createdAt)
        }.toList
        Ok(Json.obj("notes" -> notes)) 
      } else {
        InternalServerError
      }
    } else {
      BadRequest
    }
  }
  
  def requests = withAuth { userId => implicit request =>
    val status = request.getQueryString("status").getOrElse("open")
    val times = PostgreSql.parseTimes(request.getQueryString("requested").getOrElse(""))
    val email = request.getQueryString("email").getOrElse("")
    val find = if (status.matches("open")) {
      ReviewRequest.findOpen(Some(times))
    } else {
      ReviewRequest.findByClosedReason(ClosedReason.fromStr(status), times, limit)
    }
    val requests = if (email.nonEmpty) find.filter(_.email.equalsIgnoreCase(email)) else find
    val uris = Uri.find(requests.map(_.uriId)).map(uri => (uri.id, uri.uri.toString)).toMap
    Ok(views.html.requests(requests, uris, limit, User.find(userId.get).get))
    	.withCookies(cookies(request, List("status", "email", "requested")):_*)
  }
  
  def request(id: Int) = withAuth { userId => implicit request =>
    val revReq = ReviewRequest.find(id)
    if (revReq.isDefined) {
    	val others = ReviewRequest.findByUri(revReq.get.uriId).filterNot(_.id==revReq.get.id)
    	val uri = Try(Uri.find(revReq.get.uriId).get.uri.toString).getOrElse("")
    	Ok(views.html.request(revReq.get, others, uri, User.find(userId.get).get))
    } else {
      Ok(views.html.partials.modelnotfound("Request "+id))
    }
  }
  
  def newReviewRequest = withAuth { userId => implicit request =>
    Ok(views.html.newrequest())
  }
  
  def requestReview = withAuth { userId => implicit request =>
    val json = request.body.asJson
    if (json.isDefined) {
      val uris = Try(json.get.\("uris").as[String].split("\\n").toList).getOrElse(List())
      val email = json.get.\("email").asOpt[String]
      val notes = json.get.\("notes").asOpt[String]
      if (uris.nonEmpty && email.isDefined) {
        val ip = Ip.toLong(request.headers.get("X-FORWARDED-FOR").getOrElse(request.remoteAddress))
        if (uris.size > 1) {
          NotImplemented(Json.obj("msg" -> "NOT YET IMPLEMENTED (WTSN-33)"))	//TODO WTSN-33 bulk review submission
        } else {
          val uri = Uri.findBySha256(Hash.sha256(uris.head).getOrElse(""))
          if (uri.isDefined && uri.get.isBlacklisted) {
            val rr = ReviewRequest.createAndFind(uri.get.id, email.get, ip, notes)
            if (rr.isDefined) {
            	Ok(Json.obj("msg" -> "Review Requested", "id" -> rr.get.id))
            } else {
              InternalServerError(Json.obj("msg" -> "Review Request Failed"))
            }
          } else {
            BadRequest(Json.obj("msg" -> ("'"+uris.head+"' is not currently blacklisted")))
          }
        }
      } else {
        val msg = if (uris.isEmpty) "URI required" else "Email required"
        BadRequest(Json.obj("msg" -> msg))
      }
    } else {
      BadRequest
    }
  }
  
  def closeReviewRequest = withAuth { userId => implicit request =>
    val user = User.find(userId.get)
    if (user.isDefined && user.get.hasRole(Role.VERIFIER)) {
	    val json = request.body.asJson
	    val reason = Try(ClosedReason.fromStr(json.get.\("reason").as[String]).get).toOption
	    val id = json.get.\("id").asOpt[Int]
	    if (json.isDefined && reason.isDefined && id.isDefined) {
	      val rr = ReviewRequest.find(id.get)
	      if (rr.isDefined) {
	        if (rr.get.close(reason.get)) {
	          val updated = ReviewRequest.find(rr.get.id).get
	          Ok(Json.obj("closed_reason" -> updated.closedReason.get.toString, "closed_at" -> updated.closedAt))
	        } else {
	          InternalServerError
	        }
	      } else {
	        BadRequest(Json.obj("msg" -> "Review Request Not Found"))
	      }
	    } else {
	      BadRequest
	    }
    } else {
      Unauthorized
    }
  }
  
  def closeReviewRequests = withAuth { userId => implicit request =>
    val user = User.find(userId.get)
    if (user.isDefined && user.get.hasRole(Role.VERIFIER)) {
    	val json = request.body.asJson
	    val reason = Try(ClosedReason.fromStr(json.get.\("reason").as[String]).get).toOption
	    val ids = json.get.\("ids").asOpt[List[Int]].getOrElse(List())
	    if (reason.isDefined && ids.nonEmpty) {
	      val numClosed = ids.foldLeft(0) { (c, id) =>
	        val closed = Try(ReviewRequest.find(id).get.close(reason.get))
	        if (closed.isSuccess && closed.get) c + 1 else c
	      }
	      val msg = "Closed " + numClosed + " Review Requests"
	    	Ok(Json.obj("msg" -> msg))
	    } else {
	      BadRequest
	    }
    } else {
      Unauthorized
    }
  }
  
  def submitCommunityReports = withAuth { userId => implicit request =>
    val created = Try(createCommunityReports(request.body.asJson.get)).getOrElse(0)
    if (created > 0) {
    	val msg = "Created " + created + " community reports"
    	Ok(Json.obj("msg" -> msg))
    } else {
      BadRequest
    }
  }
  
  def createCommunityReports(json: JsValue): Int = {
    return try {
	    val uris = Uri.findOrCreateIds(json.\("uris").as[String].split("\\n").toList)
	    val ip = json.\("ip").asOpt[Long]
	    val desc = json.\("description").asOpt[String]
	    val badCode = json.\("bad_code").asOpt[String]
	    val crType = Try(CrType.findByType(json.\("type").as[String]).get.id).toOption
	    val crSource = Try(CrSource.findByName(json.\("source").as[String]).get.id).toOption
      CommunityReport.bulkCreate(uris, ip, desc, badCode, crType, crSource)
    } catch {
      case _: Exception => 0
    }
  }
  
  def tags = TODO	//TODO WTSN-56 view/add/toggle tags
  
  def tag(name: String) = TODO	//TODO WTSN-56 view tag
  
  def uris = TODO	//TODO WTSN-57 uris view
  
  def uri(id: Int) = TODO	//TODO WTSN-57 view uri
  
  def ips = TODO	//TODO WTSN-59 ips view
  
  def ip(ip: Long) = TODO	//TODO WTSN-59 view ip
  
  def apiKeys = withAuth { userId => implicit request =>
    val user = User.find(userId.get).get
    if (user.hasRole(Role.ADMIN)) {
	    val email = user.email
	    val keys = ApiAuth.newPair
	    if (keys.isDefined) {
	      Mailer.sendSecret(email, keys.get._2)
	    	Ok(views.html.apikeys(email, keys.get._1))
	    } else {
	      InternalServerError
	    }
    } else {
      Unauthorized
    }
  }  
  def welcome = Action { implicit request =>
    Ok(views.html.welcome())
  }
  
  def login = Action(parse.json) { implicit request =>
    val email = request.body.\("email").asOpt[String]
    val pw = request.body.\("pw").asOpt[String]
    if (email.isDefined && pw.isDefined) {
      val user = AuthAuth.authenticate(email.get, pw.get)
      if (user.isDefined) {
        user.get.updateLoginCount()
        val sessionId = newSessionId
        updateSessionExpiry(sessionId, user.get.id)
        val returnTo = Json.obj("returnTo" -> request.session.get("returnTo").getOrElse("/").toString)
        Ok(returnTo).withSession(("sessionId" -> sessionId))
      } else {
        Unauthorized
      }
    } else {
      BadRequest
    }
  }
   
  def logout = Action { implicit request =>
  	Redirect(routes.Application.welcome).withNewSession.withSession(("returnTo","/"))
  }
  
  def register = Action {
    Ok(views.html.register())
  }
  
  def createAccount = Action(parse.json) { implicit request =>
    val email = request.body.\("email").asOpt[String]
    val pw = request.body.\("pw").asOpt[String]
    if (email.isDefined && pw.isDefined) {
      val stormpathAccount = AuthAuth.create(email.get, pw.get)
      val uname = email.get.split("@").headOption
      val userModel = if (stormpathAccount && uname.isDefined) User.create(uname.get, email.get) else false
      val created = stormpathAccount && userModel
      if (created) {
        Logger.info("Account created for '"+email.get+"'")
      }
  		Ok(Json.obj("created" -> created))
    } else {
      BadRequest
    }
  }
  
  def resetPassword = Action { implicit request =>
    Ok(views.html.pwreset())
  }
  
  def sendPwResetEmail = Action(parse.json) { implicit request =>
    val email = request.body.\("email").asOpt[String]
    if (email.isDefined) {
      Ok(Json.obj("sent" -> AuthAuth.sendResetMail(email.get)))
    } else {
      BadRequest
    }
  }
  
  def untrail(path: String) = Action {
    MovedPermanently("/" + path)
  }
  
  def javascriptRoutes = Action { implicit request =>
    Ok(Routes.javascriptRouter("jsRoutes")(
      routes.javascript.Application.login,
      routes.javascript.Application.createAccount,
      routes.javascript.Application.sendPwResetEmail,
      routes.javascript.Application.requestReview,
      routes.javascript.Application.closeReviewRequests,
      routes.javascript.Application.closeReviewRequest,
      routes.javascript.Application.updateReviewStatus,
      routes.javascript.Application.updateReviewTestData,
      routes.javascript.Application.addReviewNote
		)).as("text/javascript")
  }
  
}

trait Secured {
  
  val sessionTimeoutSeconds = Try(sys.env("SESSION_TIMEOUT_MINS").toInt).getOrElse(15) * 60
  
  def withAuth(auth: => Option[Int] => Request[AnyContent] => Result) = {
    Security.Authenticated(userId, onUnauthorized) { id =>
      Action(request => auth(Some(id))(request))
    }
  }
  
  def updateSessionExpiry(sessionId: String, userId: Int) = Cache.set(sessionId, userId, sessionTimeoutSeconds)
  
  def newSessionId: String = UUID.randomUUID.toString + "-" + System.currentTimeMillis
  
  private def userId(request: RequestHeader): Option[Int] = {
    val sessionId = request.session.get("sessionId")
    val userId = Try(Cache.getAs[Int](sessionId.get).get).toOption
    return if (userId.isDefined) {
      updateSessionExpiry(sessionId.get, userId.get)
      userId
    } else {
      None
    }
  }
    
  private def onUnauthorized(request: RequestHeader) = {
    val returnTo = request.uri match {
      case "/welcome" | "/register" | "/logout" => "/"
      case _ => request.uri
    }
    Results.Redirect(routes.Application.welcome).withSession(("returnTo", returnTo))
  }
  
}