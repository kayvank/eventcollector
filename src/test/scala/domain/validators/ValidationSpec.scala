package domain.validators


import com.eclipsesource.schema._
import com.typesafe.scalalogging.LazyLogging
import domain.Domain.{EndoEvent, EndoEventWithGeo, GeoIp}
import domain.transformers.DomainJsonFormatters
import play.api.libs.json._

import scala.io.Source

/**
  * Created by kayvan on 4/27/16.
  */

class ValidationSpec extends org.specs2.mutable.Specification with LazyLogging {

  val payloadInvalid = Json.parse(
    Source.fromInputStream(getClass.getResourceAsStream("/invalid-event-data.json")).mkString
  ).asOpt[JsObject]

  val payload = Json.parse(
    Source.fromInputStream(getClass.getResourceAsStream("/event-data.json")).mkString
  ).asOpt[JsObject]

  "endo-event json formatting specificatoins".title

  "clients valida endo-events will pass schema-validattion" >> {
    val validator = EndoSchemaValidator.validate
    val endoEventUnderTest = EndoEvent(
      eventList = payload.get,
      ip = Some("0.0.0.0"),
      httpUserAgent = Some("test-browser"))
    validator(endoEventUnderTest) must beRight(endoEventUnderTest)
  }
  "clients fail invalid endo-events " >> {
    val computed = EndoSchemaValidator.validate(
      EndoEvent(
        eventList = payloadInvalid.get,
        ip = Some("0.0.0.0"),
        httpUserAgent = Some("test-browser")
      ))
    logger.info(s"computed=${computed.left.get.errMsg}")
    computed.isLeft

  }


  "valid clients endo-events are decorated with metadata & geo location" >> {
    val endo2Jsonlist: EndoEventWithGeo => List[JsObject] = DomainJsonFormatters.endoWithGeo2jsons
    val endoEventUnderTest = EndoEventWithGeo(
      EndoEvent(
        eventList = payload.get,
        ip = Some("0.0.0.0"),
        httpUserAgent = Some("test-browser")),
      GeoIp(ip_address = "0.0.0.0",
        lat = 1.00001F,
        lng = 2.0003F,
        city = "san francisco",
        region = "ca",
        postal_code = "",
        country = "us")
    )
    val computed = endo2Jsonlist(endoEventUnderTest)
    logger.info(s"computed-completed-endo= ${computed} ")
    computed.size > 0
  }
}
