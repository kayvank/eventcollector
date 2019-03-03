package api

/**
  * Created by kayvan.kazeminejad@vevo.com on 12/8/15.
  */

import info.BuildInfo
import spray.httpx.{PlayJsonSupport}
import spray.routing.{HttpServiceActor, HttpService}
import play.api.libs.json._

trait ServiceStatusApi extends HttpService with PlayJsonSupport {
  def route =
    get {
      detach() {
        complete {
          Json.obj("status" -> "OK") ++ Json.parse(BuildInfo.toJson).as[JsObject]
        }
      }
    }
}

class ServiceStatusActor extends HttpServiceActor
  with ServiceStatusApi {
  implicit val system = context.system

  import spray.routing.RejectionHandler.Default

  def receive = runRoute(route)
}
