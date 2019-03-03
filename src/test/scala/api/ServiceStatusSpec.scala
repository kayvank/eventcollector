package api

import akka.actor._
import akka.testkit._
import core.Core
import org.specs2.mutable
import play.api.libs.json._
import spray.http.StatusCodes
import spray.testkit.Specs2RouteTest

/**
  * Created by kayvan.kazeminejad@vevo.com on 6/1/16.
  */
class ServiceStatusSpec extends mutable.Specification
  with Specs2RouteTest
  with Core
  with ServiceStatusApi {

  import concurrent.duration._

  implicit def default(implicit system: ActorSystem) = {
    RouteTestTimeout(new DurationInt(5).second.dilated(system))
  }

  def actorRefFactory: ActorSystem = system

  "Service status specificaitons".title

  "Returns OK with build-info with the /status route" >> {
    Get("/") ~> route ~> check {
      status === StatusCodes.OK &&
      Json.parse(response.entity.data.asString).asOpt[JsObject].isDefined

    }
  }
}
