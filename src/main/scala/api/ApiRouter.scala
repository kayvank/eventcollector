package api

/**
  * Created by kayvan.kazeminejad@vevo.com on 12/8/15.
  **/

import akka.actor.{ActorRef}
import spray.http.StatusCodes
import spray.routing._

object ApiRouter {

  case class Register(
    name: String,
    actorRef: ActorRef
  )
}

/**
  * Register route-prefix and the route actor
  */
class ApiRouter extends HttpServiceActor {

  import ApiRouter._

  var endpoints = Map.empty[String, ActorRef]

  def createEndpointRoute(name: String, target: ActorRef): Route =
    pathPrefix(name) {
      ctx => target ! ctx
    }

  implicit val myRejectionHandler = RejectionHandler {
    case UnacceptedResponseContentTypeRejection(Seq(contentType)) :: _ =>
      complete {
        (StatusCodes.BadRequest, "Invalid Content-Type")
      }
  }

  def completeRoute: Route =
    endpoints.foldLeft[Route](reject) { (path, next) =>
      val (name, targetActor) = next
      val endpointRoute = createEndpointRoute(name, targetActor)
      endpointRoute ~ path
    }

  val registerReceive: Receive = {
    case Register(name, actor) =>
      endpoints = endpoints.updated(name, actor)
      context become receive
  }

  def receive: Receive = registerReceive orElse runRoute(completeRoute)
}
