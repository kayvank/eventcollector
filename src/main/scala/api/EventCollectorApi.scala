package api

/**
  * Created by kayvan.kazeminejad@vevo.com on 12/8/15.
  */

import akka.actor.ActorRef
import domain.Domain.EndoEvent
import play.api.libs.json._
import protocol.Protocol.{InValidEndoEvent, NotValidatedEndoEvent, ValidEndoEvent}
import spray.http._
import spray.httpx.PlayJsonSupport
import spray.routing._
import spray.http.HttpHeaders._
import domain.validators.EndoSchemaValidator
import domain.validators.EndoSchemaValidator.ValidationException

//TODO this endpoint is simply echo's payload back. it is left to support he legacy devices. Need to take it once legacy devices are at EOL
trait EventCollectorApi extends HttpService
  with PlayJsonSupport {
  def eventCollectorRoute =
    path("collector") {
      post {
        detach() {
          entity(as[JsObject]) { collectionEvent =>
            complete((StatusCodes.Accepted, collectionEvent))
          }
        }
      }
    }
}

trait EndoCollectorApi extends HttpService
  with PlayJsonSupport {

  val validator: EndoEvent => Either[ValidationException, EndoEvent]
  val theGru: ActorRef

  def endoValidatorRoute = {
    path("endo") {
      respondWithHeaders(
        RawHeader("Allow", "OPTIONS, POST"),
        RawHeader("Access-Control-Allow-Origin", "*"),
        RawHeader("Access-Control-Allow-Headers", "Content-Type, Accept")
      ) {
        post {
            detach() {
              clientIP { clientIp =>
                optionalHeaderValueByName("User-Agent") { ua =>

                  entity(as[JsObject]) { events =>
                    val endoEvent = EndoEvent(
                      eventList = events,
                      ip = clientIp.toOption.map(x => x.getHostAddress),
                      httpUserAgent = ua)

                    theGru ! NotValidatedEndoEvent(endoEvent)

                    validator(endoEvent) match {
                      case Right(e: EndoEvent) =>
                        theGru ! ValidEndoEvent(e)
                        complete((StatusCodes.Accepted, events))
                      case Left(v: ValidationException) =>
                        theGru ! InValidEndoEvent(endoEvent, v)
                        complete(StatusCodes.BadRequest, v.errMsg)
                  }
                }
              }
            }
          }
        } ~ options {
          complete(StatusCodes.OK)
        }
      }
    }
  }
}

class EventCollectorActor(gru: ActorRef) extends HttpServiceActor
  with EventCollectorApi
  with EndoCollectorApi {

  val theGru = gru
  val validator = EndoSchemaValidator.validate

  import spray.routing.RejectionHandler.Default

  def route = eventCollectorRoute ~ endoValidatorRoute

  def receive = runRoute(route)
}
