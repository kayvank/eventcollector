package protocol


import domain.Domain.{EndoEvent, EndoEventWithGeo, GeoIp, Metadata}
import domain.validators.EndoSchemaValidator.ValidationException
import akka.actor._


/**
  * Created by kayvan on 11/4/16.
  */
object Protocol {

  case class NotValidatedEndoEvent(
    endoEvent: EndoEvent
  )

  case class ValidEndoEvent(
    endoEvent: EndoEvent
  )

  case class InValidEndoEvent(
    endoEvent: EndoEvent,
    v: ValidationException
  )

  case class GeoComputationRequest(
    endoEvent: EndoEvent,
    nextMinion: ActorRef
  )
/**
  case class GeoComputationResponse(
    endoEventWithGeo: EndoEventWithGeo
  )
  * */
  case class KclPublishNotValidatedEndoRequest(
    event: NotValidatedEndoEvent
  )

  case class PublishInValidEndoRequest(
    event: InValidEndoEvent
  )


  case class PublishValidEndoRequest(
    event: EndoEventWithGeo
  )

  case class GeoIpProcessorRestartRequest()

  case class MerticsReport(
    report: String
  )
  case class DownloadMxMinDbRequest()

  case class CompletedDownloadMxMinDb()

  case class MxMinDbDownloadFailure(e: Throwable)

}
