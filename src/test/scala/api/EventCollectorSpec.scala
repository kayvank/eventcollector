
import akka.testkit._
import akka.actor._
import api.{EndoCollectorApi, EventCollectorApi}
import org.specs2.mutable
import spray.testkit.Specs2RouteTest

import concurrent.duration._
import play.api.libs.json.{JsObject, Json}
import spray.http.{HttpEntity, MediaTypes, StatusCodes}
import spray.http.HttpHeaders.`Remote-Address`
import spray.http.HttpHeaders.`User-Agent`

import scala.io.Source
import com.typesafe.scalalogging.LazyLogging
import domain.validators.EndoSchemaValidator


/**
  * Created by Kayvan Kazeminejad on 12/15/15.
  */


class EventCollectorSpec
  extends mutable.Specification
  with Specs2RouteTest
  with EventCollectorApi
  with EndoCollectorApi
  with LazyLogging {

  implicit def default(implicit system: ActorSystem) = {
    RouteTestTimeout(new DurationInt(5).second.dilated(system))
  }
  def actorRefFactory: ActorSystem = system
  val theGru = TestProbe().ref
  val validator = EndoSchemaValidator.validate
  "Event Collector Spec".title
  val eventData = Json.parse(
    Source.fromInputStream(getClass.getResourceAsStream("/event-data.json")).mkString
  ).as[JsObject]
  "returns OK when event data POSTed to /collector" >> {
    val formData = Some(HttpEntity(
      MediaTypes.`application/json`,
      Json.stringify(eventData)))
    Post("/collector", formData) ~> eventCollectorRoute ~> check {
      status === StatusCodes.Accepted
    }
  }

  "returns BAD MEDIA TYPE for incorrect media-type" in {
    val formData = Some(HttpEntity(
      MediaTypes.`multipart/form-data`,
      Json.stringify(eventData)))
    Post("/endo", formData)
      .withHeaders(`Remote-Address`("0.0.0.0")
      ) ~> sealRoute(endoValidatorRoute) ~> check {
      (status === StatusCodes.UnsupportedMediaType)
    }
  }

  "return 202 ACCEPTED when event data is Posted to /endo with user-agent" >> {
    val httpEntity = HttpEntity(MediaTypes.`application/json`, Json.stringify(eventData))
    Post("/endo", Some(httpEntity))
      .withHeaders(`Remote-Address`("0.0.0.0"), `User-Agent`("user-agent-123")) ~>
      sealRoute(endoValidatorRoute) ~> check {
      status must be equalTo (StatusCodes.Accepted)
    }
  }

  "return 202 ACCEPTED when event data is Posted to /endo with no user-agent" >> {
    val httpEntity = HttpEntity(MediaTypes.`application/json`, Json.stringify(eventData))
    Post("/endo", Some(httpEntity))
      .withHeaders(`Remote-Address`("0.0.0.0")) ~>
      sealRoute(endoValidatorRoute) ~> check {
      status must be equalTo (StatusCodes.Accepted)
    }
  }

  "return 400 BAD REQUEST when event data is invalid" >> {
    val invalidJson =
      """
        {
          "device_context": {"user_id": "fakeuser"},
          "events": [
            {
              "verb": "some verb",
              "noun": 2
            }
          ]
        }
      """

    val httpEntity = HttpEntity(MediaTypes.`application/json`, invalidJson)

    Post("/endo", Some(httpEntity))
      .withHeaders(`Remote-Address`("0.0.0.0")) ~>
      sealRoute(endoValidatorRoute) ~> check {
      status must be equalTo (StatusCodes.BadRequest)
    }
  }
}
