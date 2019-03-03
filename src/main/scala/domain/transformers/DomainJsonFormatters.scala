package domain.transformers

import domain.Domain._
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import protocol.Protocol.{InValidEndoEvent, NotValidatedEndoEvent, ValidEndoEvent}

/**
  * Created by kayvan on 11/4/16.
  */
object DomainJsonFormatters {

  import play.api.libs.json._

  implicit val metadataWrites: Writes[Metadata] = (
    (__ \ 'event_type).write[String] and
      (__ \ 'app_name).write[String] and
      (__ \ 'app_version).write[String] and
      (__ \ 'build_number).write[String] and
      (__ \ 'timestamp).write[Long]
    ) (unlift(Metadata.unapply))

  implicit val metadataAsJson: Metadata => JsObject = metadata =>
    Json.obj("metadata" -> Json.obj(
      "event_type" -> metadata.event_type,
      "app_name" -> metadata.app_name,
      "app_version" -> metadata.app_version,
      "build_number" -> metadata.build_number,
      "timestamp" -> metadata.timestamp))

  implicit val geoIpWriter: Writes[GeoIp] = (
    (__ \ 'ip_address).write[String] and
      (__ \ 'lat).write[Float] and
      (__ \ 'lng).write[Float] and
      (__ \ 'city).write[String] and
      (__ \ 'region).write[String] and
      (__ \ 'postal_code).write[String] and
      (__ \ 'country).write[String]
    ) (unlift(GeoIp.unapply))

  val sentAtAsJson: JsObject => JsObject = endoEvents => {
    val sentAtTransformer = (__ \ 'sent_at).json.pickBranch
    endoEvents.transform(sentAtTransformer).asOpt.getOrElse(Json.obj())
  }
  val deviceContextAsJson: JsObject => JsObject = endoEvents => {
    val deviceContextTransformer = (__ \ 'device_context).json.pickBranch
    endoEvents.transform(deviceContextTransformer).asOpt.getOrElse(Json.obj())
  }
  val endoAsJsonList: JsObject => List[JsObject] = eventJson => {
    val eventsTransformer = (__ \ 'events).json.pick[JsArray]
    eventJson.transform(eventsTransformer).asOpt.get.value.map(
      j => Json.obj("event_data" -> j)).toList
  }
  val endoWithGeo2jsons: EndoEventWithGeo => List[JsObject] = event => {
    val geoJson =
      Json.toJson(event.geo).asOpt[JsObject].getOrElse(Json.obj())
    val metadataJson =
      Json.toJson(event.endoEvent.metadata).asOpt[JsObject].getOrElse(Json.obj())
    val deviceContextJson =
      deviceContextAsJson(event.endoEvent.eventList).asOpt[JsObject].getOrElse(Json.obj())
   val sentAtJson =
      sentAtAsJson(event.endoEvent.eventList).asOpt[JsObject].getOrElse(Json.obj())

    val endoEventsData = endoAsJsonList(event.endoEvent.eventList)

    endoEventsData.map(e =>
      Json.obj("metadata" -> metadataJson) ++
        Json.obj("event_id" -> java.util.UUID.randomUUID.toString) ++
        e ++
        deviceContextJson ++ sentAtJson ++
        Json.obj("geo_ip" -> geoJson) ++
        event.endoEvent.httpUserAgent.map(ua => Json.obj("http_user_agent" -> ua)
        ).getOrElse(Json.obj()) ++
        event.endoEvent.ip.map(ipAddress => Json.obj("ip_address" -> ipAddress)
        ).getOrElse(Json.obj()))
  }

  def endoEvent2Json(event: EndoEvent) = {
    val metadataJson =
      Json.toJson(event.metadata).asOpt[JsObject].getOrElse(Json.obj())
    Json.obj("metadata" -> metadataJson) ++
      Json.obj("event_id" -> event.event_id) ++
      event.httpUserAgent.map(ua => Json.obj("http_user_agent" -> ua)
      ).getOrElse(Json.obj()) ++
      event.ip.map(ipAddress => Json.obj("ip_address" -> ipAddress)
      ).getOrElse(Json.obj()) ++ event.eventList
  }

  val notValidatedEndoEvent2Json: NotValidatedEndoEvent => JsObject = event =>
    endoEvent2Json(event.endoEvent)

  val inValidEndoEvent2Json: InValidEndoEvent => JsObject = event =>
    endoEvent2Json(event.endoEvent) ++ Json.obj("validation_exception" -> event.v.errMsg)

}
