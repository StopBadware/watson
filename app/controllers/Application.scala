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
import models.enums.ClosedReason

object Application extends Controller with JsonMapper with Secured with Cookies {
  
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
      val email = Try(json.get.\("email").as[String]).toOption
      val notes = Try(json.get.\("notes").as[String]).toOption
      if (uris.nonEmpty && email.isDefined) {
        if (uris.size > 1) {
          NotImplemented("NOT YET IMPLEMENTED (WTSN-33)")	//TODO WTSN-33 bulk review submission
        } else {
          val uri = Uri.findBySha256(Hash.sha256(uris.head).getOrElse(""))
          if (uri.isDefined && uri.get.isBlacklisted) {
//            val ip = Ip.toLong(request.remoteAddress)	//DELME
            val ip = request.headers.get("X-FORWARDED-FOR").getOrElse(request.remoteAddress)
            println(request.remoteAddress, ip, Ip.toLong(ip))		//DELME WTSN-58
          	//TODO WTSN-58 convert IP to long
          	//TODO WTSN-58 submit review request
          	Ok(Json.obj("msg" -> "TODO WTSN-58"))
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
  
  def tags = TODO	//TODO WTSN-56 view/add/toggle tags
  
  def tag(name: String) = TODO	//TODO WTSN-56 view tag
  
  def uris = TODO	//TODO WTSN-57 uris view
  
  def uri(id: Int) = TODO	//TODO WTSN-57 view uri
  
  def ips = TODO	//TODO WTSN-59 ips view
  
  def ip(ip: Long) = TODO	//TODO WTSN-59 view ip  
  
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
      routes.javascript.Application.requestReview
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
    val userId = Try(Cache.getAs[Int](sessionId.get)).getOrElse(None)
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