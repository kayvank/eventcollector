package workers

import akka.actor.SupervisorStrategy.Restart
import akka.actor.{Actor, ActorLogging, ActorRef, AllForOneStrategy, Props}
import api.ApiRouter.Register
import api.{EventCollectorActor, ServiceStatusActor}
import kinesis.KinesisPublisher
import protocol.Protocol.ValidEndoEvent

import scala.concurrent.duration._

/**
  * Created by kayvan on 11/4/16.
  */

class EndoProcessingSupervisor(
  apiRouter: ActorRef,
  probe: Option[ActorRef]
) extends Actor
  with ActorLogging {
  

  val serviceStatusActor =
    context.actorOf(Props[ServiceStatusActor], "serviceStatusActor")

  val geoComputationMinionProxy =
    context.actorOf(Props(classOf[GeoComputationMinionProxy], None), "geoComputationMinion")

  val kclMinionProxy = context.actorOf(Props(classOf[KclMinionProxy],
    KinesisPublisher.publish,
    KinesisPublisher.publishList,
    None), "kclMinionProxy")

  val gru = context.actorOf(Props(classOf[Gru],
    kclMinionProxy,
    geoComputationMinionProxy,
    None),
    "gru")

  val eventCollectorApi =
    context.actorOf(Props(classOf[EventCollectorActor], gru), "eventCollectorApi")

  val maxMindProxyMinion = context.actorOf(Props(classOf[MaxMindGeoDbMinionProxy], gru, None), "maxMindDbMinion")
  override def preStart(): Unit = {
    apiRouter ! Register("event", eventCollectorApi)
    apiRouter ! Register("status", serviceStatusActor)
    super.preStart()
  }

  override val supervisorStrategy =
    AllForOneStrategy(maxNrOfRetries = -1, withinTimeRange = 3 seconds) {
      case e: Exception =>
        log.error(s"actor ${self.path} received Exception ${e.getMessage} from ${sender.path}")
        e.printStackTrace()
        log.warning(s"${self.path} will restart children ${gru.path} ${geoComputationMinionProxy.path}  ${serviceStatusActor.path} ${eventCollectorApi.path} ${maxMindProxyMinion.path} ")
        Restart
    }

  def receive = {
    case event: ValidEndoEvent => gru forward (event)
  }
}
