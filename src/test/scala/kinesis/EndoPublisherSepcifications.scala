package kinesis

import java.nio.ByteBuffer

import com.typesafe.scalalogging.LazyLogging
import domain.Domain.{EndoEvent, EndoEventWithGeo, GeoIp}
import domain.validators.EndoSchemaValidator.ValidationException
import play.api.libs.json.{JsObject, Json}
import protocol.Protocol.{InValidEndoEvent, NotValidatedEndoEvent}
import utils.Global

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.io.Source
import KCLType._

/**
  * Created by kayvan on 11/6/16.
  */
class EndoPublisherSepcifications extends org.specs2.mutable.Specification
  with LazyLogging {

  val payload = Json.parse(
    Source.fromInputStream(getClass.getResourceAsStream("/event-data.json")).mkString
  ).asOpt[JsObject]
  val endoEvent = EndoEvent(
    eventList = payload.get,
    ip = Some("204.28.115.197"),
    httpUserAgent = Some("test-browser"))

  val endoEventWithGeo = EndoEventWithGeo(endoEvent, GeoIp(
    ip_address = "204.28.115.197",
    lat = 1.001F,
    lng = 1.001F,
    city = "San Frandcisco",
    region = "CA",
    postal_code = "989898",
    country = "usa"))

  implicit val kclPublish: String => KCLData => Future[String] = streamName => payload => {
    Future {
      new java.lang.String(payload.value.array(), "utf-8")
    }
  }

  implicit val kclPublishList: String => List[KCLData] => Future[List[String]] = streamName => payload => {
    Future {
      payload.map(p => new java.lang.String(p.value.array(), "utf-8"))
    }
  }

  val waitTime = Duration("5 second")

  "kcl publisher data converters specifications".title

  "transoform a VALID endoEvent to a publishable Kinesis payload" >> {

    val computed: List[String] = Await.result(EndoEventPublisher.endoEventPublisher(endoEventWithGeo), waitTime)
    logger.info(s"kcl computed endoEventWithGeo result = ${computed}")
    computed.size > 0
  }

  "transoform a INVALID endoEvent to a publishable Kinesis payload" >> {
    val invalidEvent = InValidEndoEvent(endoEvent, ValidationException(Json.obj("error" -> "invalid-exception")))
    val computed = Await.result(EndoEventPublisher.endoEventPublisher(invalidEvent), waitTime)
    logger.info(s"kcl computed result for invalidEndo = ${computed}")
    Json.parse(computed).asOpt[JsObject].isDefined
  }

  "transoform a NOT-VALIDATED endoEvent to a publishable Kinesis payload" >> {
    val invalidEvent = NotValidatedEndoEvent(endoEvent)
    val computed = Await.result(EndoEventPublisher.endoEventPublisher(invalidEvent), waitTime)
    logger.info(s"kcl computed result for Not-validated = ${computed}")
    Json.parse(computed).asOpt[JsObject].isDefined
  }
}
