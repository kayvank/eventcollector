package domain

import info.BuildInfo
import play.api.libs.json.JsObject

/**
  * Created by kayvan on 11/4/16.
  */

object Domain {

  //TODO at this time the lamdas processing kinesis & extract jobs dont support "Option"
  //
  case class GeoIp(
    ip_address: String,
    lat: Float,
    lng: Float,
    city: String, // shouldbe Option. down-stream doesnt support this.
    region: String, // shouldbe Option. down-stream doesnt support this.
    postal_code: String, // shouldbe Option. down-stream doesnt support this.
    country: String)

  case class Metadata(
    event_type: String = "endo-1.0",
    app_name: String = BuildInfo.name,
    app_version: String = BuildInfo.version,
    build_number: String = BuildInfo.buildInfoBuildNumber.toString,
    timestamp: Long = System.currentTimeMillis)

  case class EndoEvent(
    metadata: Metadata = Metadata(),
    event_id: String = java.util.UUID.randomUUID.toString,
    eventList: JsObject,
    ip: Option[String],
    httpUserAgent: Option[String]
  )

  case class EndoEventWithGeo(
    endoEvent: EndoEvent,
    geo: GeoIp
  )


object EndoValidationState {

  sealed trait StateOfValidation

  case object Valid extends StateOfValidation

  case object InValid extends StateOfValidation

  case object Undefined extends StateOfValidation

}

  import EndoValidationState._

  case class RawEndoEvent(
    eventJson: JsObject,
    ip: Option[String],
    httpUserAgent: Option[String],
    validationState: Option[StateOfValidation])
}

