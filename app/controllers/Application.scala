package controllers

import java.util.UUID
import play.api._
import play.api.mvc._
import play.api.libs.json._
import play.api.cache.Cache
import play.api.Play.current
import scala.util.Try
import scalaj.http.Http
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
    	  val u = user.get.id
  			val updated = status.get match {
    	    case ReviewStatus.PENDING_BAD => r.markPendingBad(u)
    	    case ReviewStatus.CLOSED_CLEAN => r.closeClean(u)
		      case ReviewStatus.CLOSED_WITHOUT_REVIEW => r.closeWithoutReview(u)
		      case ReviewStatus.CLOSED_BAD => r.verifyBad(u)
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
          val updated = review.markPendingBad(user.get.id)
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
  
  def addNote = withAuth { userId => implicit request =>
    val json = request.body.asJson
    val user = User.find(userId.get)
    val id = json.get.\("id").asOpt[Int]
    val note = json.get.\("note").asOpt[String]
    val model = json.get.\("model").asOpt[String]
    if (json.isDefined && user.isDefined && id.isDefined && note.isDefined && model.isDefined) {
      val created = model.get match {
        case "review" => ReviewNote.create(id.get, user.get.id, note.get)
        case "cr" => CrNote.create(id.get, user.get.id, note.get)
        case _ => false
      }
      if (created) {
        val notes = (model.get match {
	        case "review" => ReviewNote.findByReview(id.get)
	        case "cr" => CrNote.findByCr(id.get)
	        case _ => List.empty[Note]
        }).map { n =>
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
    val find = status match {
      case "open" => ReviewRequest.findOpen(Some(times))
      case "all" => ReviewRequest.findOpen(Some(times)) ++ ReviewRequest.findByClosedReason(None, times, limit)
      case _ => ReviewRequest.findByClosedReason(ClosedReason.fromStr(status), times, limit)
    }
    val requests = (if (email.nonEmpty) find.filter(_.email.equalsIgnoreCase(email)) else find).splitAt(limit)._1
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
  
  def communityReports = withAuth { userId => implicit request =>
    val crSource = Try(CrSource.findByName(request.getQueryString("source").get).get.id).toOption
    val times = PostgreSql.parseTimes(request.getQueryString("reported").getOrElse(""))
    Ok(views.html.crs(CommunityReport.findSummaries(None, crSource, times, limit), limit))
    	.withCookies(cookies(request, List("type", "source", "reported")):_*)
  }
  
  def communityReport(id: Int) = withAuth { userId => implicit request =>
    val cr = CommunityReport.find(id)
    if (cr.isDefined) {
      val uriId = cr.get.uriId
      val summaries = CommunityReport.findSummariesByUri(uriId)
      val events = BlacklistEvent.findByUri(uriId)
    	val uri = Uri.find(uriId).get.uri
    	Ok(views.html.cr(cr.get, uri, summaries, events))
    } else {
      Ok(views.html.partials.modelnotfound("Community Report "+id))
    }
  }
  
  def newCommunityReport = withAuth { userId => implicit request =>
    Ok(views.html.newcr())
  }
  
  def submitCommunityReports = withAuth { userId => implicit request =>
    val created = Try(createCommunityReports(request.body.asJson.get)).getOrElse(0)
    if (created > 0) {
    	val msg = "Added " + created + " community " + (if (created==1) "report" else "reports")
    	Ok(Json.obj("msg" -> msg))
    } else {
      BadRequest
    }
  }
  
  def createCommunityReports(json: JsValue): Int = {
    return try {
	    val uriIds = Uri.findOrCreateIds(json.\("uris").as[String].split("\\n").toList)
	    val ip = Try(Ip.toLong(json.\("ip").as[String]).get).toOption
	    val desc = json.\("description").asOpt[String]
	    val badCode = json.\("bad_code").asOpt[String]
	    val crType = Try(CrType.findByType(json.\("type").as[String]).get.id).toOption
	    val crSource = Try(CrSource.findByName(json.\("source").as[String]).get.id).toOption
      CommunityReport.bulkCreate(uriIds, ip, desc, badCode, crType, crSource)
    } catch {
      case _: Exception => 0
    }
  }
  
  def rescans = withAuth { userId => implicit request =>
    Ok(views.html.rescans(Redis.getGoogleRescanQueue.size))	
  }
  
  def addToRescanQueue = withAuth { userId => implicit request =>
    val json = request.body.asJson
    val uriIds = Try(Uri.findIds(json.get.\("uris").as[String].split("\\n").toList)).getOrElse(List())
    if (uriIds.nonEmpty) {
    	val blacklistedIds = uriIds.intersect(BlacklistEvent.blacklistedUriIdsEventIds(Source.GOOG).keys.toList)
    	val added = Uri.find(blacklistedIds).map(_.uri).foldLeft(0) { (cnt, uri) =>
    	  if (Redis.addToGoogleRescanQueue(uri)) cnt + 1 else cnt
    	}
    	Ok(Json.obj("added" -> added, "count" -> Redis.getGoogleRescanQueue.size))
    } else {
      BadRequest
    }
  }
  
  def users = withAuth { userId => implicit request =>
    Ok(views.html.users(User.find(userId.get).get))
  }
  
  def tags = TODO	//TODO WTSN-56 view/add/toggle tags
  
  def tag(name: String) = TODO	//TODO WTSN-56 view tag
  
  def uris = withAuth { userId => implicit request =>
    val chUris = try {
	    val searchFor = new ReportedUri(request.getQueryString("search").get)
	    Try(Clearinghouse.findUrisWithSiblingsAndChildren(searchFor.uri.getHost)).getOrElse(List()).partition(_.blacklisted)
    } catch {
      case _: Exception => (List(), List())
    }
    val blistedBy = (BlacklistEvent.urisBlacklistedBy(chUris._1.map(_.uriId)) ++ chUris._2.map(_.uriId).map(_ -> List()))
  		.map{case (id, sources) => id -> sources.map(_.abbr)}.toMap
    Ok(views.html.uris((chUris._1 ++ chUris._2).splitAt(limit)._1, blistedBy, limit)).withCookies(cookies(request, List("search")):_*)
  }
  
  def uri(id: Int) = withAuth { userId => implicit request =>
    val uri = Uri.find(id)
    if (uri.isDefined) {
      Ok(views.html.uri(uri.get))
    } else {
      Ok(views.html.partials.modelnotfound("URI "+id))
    }
  }
  
  def ips = TODO	//TODO WTSN-59 ips view
  
  def ip(ip: Long) = TODO	//TODO WTSN-59 view ip
  
  def asns = TODO	//TODO WTSN-60 asns view
  
  def asn(asn: Int) = TODO	//TODO WTSN-60 view asn
  
  def requesters = withAuth { userId => implicit request =>
    val email = request.getQueryString("email")
    val times = PostgreSql.parseTimes(request.getQueryString("requested").getOrElse(""))
    val requesters = ReviewRequest.findGroupedByRequester(times, email, limit)
    Ok(views.html.requesters(requesters, limit, User.find(userId.get).get))
    	.withCookies(cookies(request, List("email", "requested")):_*)
  }
  
  def responses = withAuth { userId => implicit request =>
    Ok(views.html.responses(User.find(userId.get).get))
  }
  
  def newQuestion = withAuth { userId => implicit request =>
    Ok(views.html.newquestion(User.find(userId.get).get))
  }
  
  def addResponse = withAuth { userId => implicit request =>
    val user = User.find(userId.get)
    if (user.isDefined && user.get.hasRole(Role.VERIFIER)) {
    	val json = request.body.asJson
	    val question = json.get.\("question").asOpt[String]
	    val answers = json.get.\("answers").asOpt[List[String]].getOrElse(List())
	    if (question.isDefined && answers.size >= 2) {
	      RequestQuestion.create(question.get)
	      val q = RequestQuestion.findByText(question.get)
	      if (q.isDefined) {
	        answers.foreach(RequestAnswer.create(_, q.get.id))
	        Ok
	      } else {
	      	BadRequest
	      }
	    } else {
	      BadRequest
	    }
    } else {
      Unauthorized
    }
  }
  
  def toggleResponse = withAuth { userId => implicit request =>
    val user = User.find(userId.get)
    if (user.isDefined && user.get.hasRole(Role.VERIFIER)) {
    	val json = request.body.asJson
	    val id = json.get.\("id").asOpt[String].getOrElse("")
	    val disable = json.get.\("disable").asOpt[Boolean]
	    try {
	      val dbId = id.split("-").last.toInt
	      val toggled = if (id.startsWith("question")) {
	        if (disable.get) RequestQuestion.find(dbId).get.disable else RequestQuestion.find(dbId).get.enable
	      } else if (id.startsWith("answer")) {
	        if (disable.get) RequestAnswer.find(dbId).get.disable else RequestAnswer.find(dbId).get.enable
	      } else {
	        false
	      }
	      if (toggled) Ok else BadRequest
	    } catch {
	      case _: Exception => BadRequest
	    }
    } else {
      Unauthorized
    }
  }
  
  def toggleRole = withAuth { userId => implicit request =>
    val user = User.find(userId.get)
    if (user.isDefined && (user.get.hasRole(Role.VERIFIER) || user.get.hasRole(Role.ADMIN))) {
    	val json = request.body.asJson
    	try {
		    val targetUser = User.find(json.get.\("user_id").as[Int]).get
    	  val role = Role.fromStr(json.get.\("role").as[String]).get
		    val removeRole = json.get.\("remove").as[Boolean]
	      val toggled = if (removeRole) targetUser.removeRole(role) else targetUser.addRole(role)
	      if (toggled) Ok else BadRequest
	    } catch {
	      case _: Exception => BadRequest
	    }
    } else {
      Unauthorized
    }
  }
  
  def toggleAbusive = withAuth { userId => implicit request =>
    val user = User.find(userId.get)
    if (user.isDefined && (user.get.hasRole(Role.VERIFIER) || user.get.hasRole(Role.ADMIN))) {
    	val json = request.body.asJson
    	try {
    	  val email = json.get.\("email").as[String]
		    val flagAbusive = json.get.\("flag").as[Boolean]
	      val toggled = if (flagAbusive) {
	        AbusiveRequester.flag(email, userId.get)
	        AbusiveRequester.isFlagged(email)
	      } else {
	        AbusiveRequester.unFlag(email, userId.get)
	        !AbusiveRequester.isFlagged(email)
	      }
	      if (toggled) Ok else BadRequest
	    } catch {
	      case _: Exception => BadRequest
	    }
    } else {
      Unauthorized
    }
  }
  
  def googleSbd = withAuth { userId => implicit request =>
    Ok(views.html.googlesbd())
  }
  
  def checkSbd = withAuth { userId => implicit request =>
    val json = request.body.asJson
    val uris = Try(json.get.\("uris").as[String].split("\n").toList).getOrElse(List())
    if (json.isDefined && uris.nonEmpty) {
      val sbdUri = "https://sb-ssl.google.com/safebrowsing/api/lookup?client=watsonsbw&apikey=" +
    		sys.env("GOOG_SBD_KEY") + "&appver=1&pver=3.0"
  		val results = uris.grouped(500).foldLeft(Map.empty[String, String]) { (map, group) =>
	    	val body = (group.+:(group.size)).mkString("\n")
	    	val request = Http.postData(sbdUri, body)
	    	map ++ (request.responseCode match {
	    	  case 200 => {
	    	    val verdicts = request.asString.split("\n").toList
	    	    verdicts.indices.map(i => group(i) -> verdicts(i)).toMap
	    	  }
	    	  case 204 => group.map(_ -> "ok").toMap
	    	  case _ => {
	    	    Logger.error("Received HTTP status " + request.responseCode + " checking Google SBD")
	    	    Map.empty[String, String]
	    	  }
	    	})
      }
      Ok(Json.obj("results" -> results))
    } else {
      BadRequest
    }
  }  
  
  def emailTemplates = withAuth { userId => implicit request =>
    Ok(views.html.emailtemplates(User.find(userId.get).get.email))
  }
  
  def sendEmailTemplatePreview = withAuth { userId => implicit request =>
    val user = User.find(userId.get)
    if (user.isDefined && user.get.hasRole(Role.USER)) {
    	val json = request.body.asJson
	    val subject = json.get.\("subject").asOpt[String]
	    val body = json.get.\("body").asOpt[String]
	    val templateName = json.get.\("template").asOpt[String]
	    if (subject.isDefined && body.isDefined && templateName.isDefined) {
	    	val sampleUri = "http://www.example.com/"
	    	val sampleCode = "<iframe src=\"http://ducksarethebest.com/\"></iframe>"
	      val sent = Mailer.sendEmail(subject.get, body.get, User.find(userId.get).get.email, sampleUri, sampleCode).apply
	      if (sent) Ok else InternalServerError
	    } else {
	      BadRequest
	    }
    } else {
      Unauthorized
    }
  }  
  
  def updateEmailTemplate = withAuth { userId => implicit request =>
    val user = User.find(userId.get)
    if (user.isDefined && user.get.hasRole(Role.USER)) {
    	val json = request.body.asJson
	    val subject = json.get.\("subject").asOpt[String]
	    val body = json.get.\("body").asOpt[String]
	    val templateName = json.get.\("template").asOpt[String]
	    if (subject.isDefined && body.isDefined && templateName.isDefined) {
	      val updated = EmailTemplate.find(templateName.get).get.update(subject.get, body.get, userId)
	      if (updated) Ok else InternalServerError
	    } else {
	      BadRequest
	    }
    } else {
      Unauthorized
    }
  }
  
  def apiKeys = withAuth { userId => implicit request =>
    val user = User.find(userId.get).get
    if (user.hasRole(Role.ADMIN)) {
	    val email = user.email
	    val keys = ApiAuth.newPair
	    if (keys.isDefined) {
	      Mailer.sendPublicKey(email, keys.get._1)
	    	Ok(views.html.apikeys(email, keys.get._2))
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
      routes.javascript.Application.submitCommunityReports,
      routes.javascript.Application.addNote,
      routes.javascript.Application.addResponse,
      routes.javascript.Application.toggleAbusive,
      routes.javascript.Application.toggleResponse,
      routes.javascript.Application.toggleRole,
      routes.javascript.Application.sendEmailTemplatePreview,
      routes.javascript.Application.updateEmailTemplate,
      routes.javascript.Application.checkSbd,
      routes.javascript.Application.addToRescanQueue
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