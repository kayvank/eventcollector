package workers

import akka.actor.SupervisorStrategy.Restart
import akka.actor._
import domain.Domain.{EndoEventWithGeo, GeoIp}
import domain.MaxMindGeoDb
import domain.transformers.Ip2GeoConverter
import protocol.Protocol._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Created by kayvan on 11/4/16.
  */
case class RestartException(msg: String) extends Throwable

class GeoComputationMinionProxy(
  probe: Option[ActorRef] = None) extends Actor
  with ActorLogging {

  val minionProps = Props(classOf[GeoComputationMinion], probe)
  val minion =
    context.actorOf(minionProps, "geoComputationMinion")

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = -1, withinTimeRange = 3 second) {
      case _ =>
        log.info(s"Restaring child ${minion.path}")
        Restart
    }

  def receive = {
    case e: Any => minion forward e
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    log.info(s"${self.path} has restarted. Restart reason: ${reason.getMessage}")
    super.preRestart(reason, message)
  }
}

class GeoComputationMinion(
  probe: Option[ActorRef] = None) extends Actor
  with ActorLogging {

  val maxMindDb = new MaxMindGeoDb()

  import scala.concurrent.ExecutionContext.Implicits.global

  def receive: Receive = {

    case GeoIpProcessorRestartRequest() =>
      log.info(s"${self.path} received GeoIpProcessorRestartRequest")
      throw RestartException("restarting geoipMinion to reset readers.")

    case request: GeoComputationRequest =>
      val nextMinion = request.nextMinion
      val goeo = request.endoEvent match {
        case endoEv if endoEv.ip.isEmpty =>
          Future {
            nulGeo(ip = "")
          }
        case endoEv if endoEv.ip.isDefined =>
          Ip2GeoConverter.ip2futureGeo(endoEv.ip.get)(maxMindDb.ip2ipLocation)
      }
      goeo foreach (g => {
        nextMinion ! EndoEventWithGeo(
          endoEvent = request.endoEvent, geo = g) })
  }

  override def preStart(): Unit = {
    log.info(s"${self.path} STARTED. !!")
    super.preStart()
  }

  override def postStop(): Unit = {
    log.info(s"${self.path} stopped.")
    super.postStop()
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    probe.foreach(_ ! GeoIpProcessorRestartRequest())
    log.info(s"${self.path} has restarted. Restart reason: ${reason.getMessage}")
    super.preRestart(reason, message)
  }

  def nulGeo(ip: String) = GeoIp(ip_address = ip,
    lat = 0F,
    lng = 0F,
    city = "",
    region = "",
    postal_code = "",
    country = "")
}
