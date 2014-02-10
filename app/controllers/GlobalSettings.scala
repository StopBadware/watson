import play.api._
import play.api.mvc._
import play.api.mvc.Results._
import play.api.Play.current

object Global extends GlobalSettings {
  
  /**
   * Force SSL in PRODUCTION mode 
   */
  override def onRouteRequest(request: RequestHeader): Option[Handler] = {
    if (Play.isProd && !request.headers.get("X-Forwarded-Proto").getOrElse("").startsWith("https")) {
      Some(Action(MovedPermanently("https://"+request.host+request.uri)))
    } else {
      super.onRouteRequest(request)
    }
  }
  
}