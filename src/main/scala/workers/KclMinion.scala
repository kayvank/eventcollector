package workers

import akka.actor.SupervisorStrategy.Restart
import akka.actor._
import kinesis.KCLType._
import protocol.Protocol._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by kayvan on 11/4/16.
  */
class KclMinionProxy(
  publisher:  String => KCLData => Future[String],
  listPublisher: String => List[KCLData] => Future[List[String]],
  probe: Option[ActorRef] = None
) extends Actor
  with ActorLogging {

  case class RestartKcl(msg: String = "Restart KCL") extends Throwable

  val minionProps =
    Props(classOf[KclMinion], publisher, listPublisher, None)
  val minion =
    context.actorOf(minionProps, "kclMinion")

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = -1, withinTimeRange = 30 seconds) {

      case _ => Restart
    }

  def receive: Receive = {

    case event: Any =>
      minion forward event
  }
}

class KclMinion(
  publisher: String => KCLData => Future[String],
  listPublisher: String => List[KCLData] => Future[List[String]] ,
  probe: Option[ActorRef] = None
) extends Actor with ActorLogging {

  import kinesis.EndoEventPublisher._
  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val kclPublish = publisher
  implicit val kclPublishList = listPublisher

  def receive: Receive = {

    case event: NotValidatedEndoEvent =>
      endoEventPublisher(event) 

    case event: domain.Domain.EndoEventWithGeo =>
      endoEventPublisher(event)

    case event: PublishInValidEndoRequest =>
      endoEventPublisher(event.event)
  }
}
