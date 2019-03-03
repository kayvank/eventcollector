package workers

import akka.actor._
import com.typesafe.scalalogging.LazyLogging
import protocol.Protocol.{CompletedDownloadMxMinDb, DownloadMxMinDbRequest, GeoIpProcessorRestartRequest, MxMinDbDownloadFailure}
import scala.concurrent.ExecutionContext.Implicits.global
import utils._
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * Created by kayvan.kazeminejad@vevo.com on 5/22/16.
  */



class MaxMindGeoDbMinionProxy(
  gru: ActorRef,
  probe: Option[ActorRef] = None) extends Actor
  with LazyLogging {

  import akka.actor.SupervisorStrategy._
  import scala.concurrent.ExecutionContext.Implicits.global

  val minion = context.actorOf(Props(classOf[MaxMindGeoDbMinion],
    gru,
    probe), "maxMindDBMinionWorker")

  override val supervisorStrategy =
    AllForOneStrategy(maxNrOfRetries = -1,
      withinTimeRange = 30 minute,
      loggingEnabled = true) {
      case e: Exception =>
        logger.error(s"Exception occred in ${self.path}. Exception = ${e.getMessage}")
        logger.error(s"Restart in ${minion.path}")
        e.printStackTrace()
        Restart
    }

  def receive: Receive = {

    case e => minion forward e
  }
}

class MaxMindGeoDbMinion(
  gru: ActorRef,
  probe: Option[ActorRef] = None) extends Actor
  with URLIO
  with ActorLogging {

  object MxMindConfig {
    import utils.Global._
    import scala.concurrent.ExecutionContext.Implicits.global

    val downloadInterval: FiniteDuration =
      cfgVevo.getInt("quartz.scheduler.maxmindb.day") day
    val path: String = cfgVevo.getString("geoip.path")
    val file: String = cfgVevo.getString("geoip.file")
  }

  def receive: Receive = {
    case CompletedDownloadMxMinDb() =>
      gru ! GeoIpProcessorRestartRequest()
      val _ = context.system.scheduler.scheduleOnce(MxMindConfig.downloadInterval, self, DownloadMxMinDbRequest())

    case DownloadMxMinDbRequest() =>
      downLoadMaxMinDB(MxMindConfig.path, MxMindConfig.file) match {
        case Success(s) =>
          self ! CompletedDownloadMxMinDb()
          log.info(s"MaxMindDB download was successfull.")

        case Failure(e) =>
          gru ! MxMinDbDownloadFailure(e)
          log.error(s"maxmind db download failed with error: ${e.getMessage}")
          e.printStackTrace()
          throw e
      }
  }

  override def postStop(): Unit = {
    log.info(s"${self.path} stopped.")
    super.postStop()
  }

  override def preStart(): Unit = {
    log.info(s"${self.path} has started.")
    self ! DownloadMxMinDbRequest()
    super.preStart()
  }

  override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
    log.info(s"${self.path} has restarted. Restart reason: ${reason.getMessage}")
    super.preRestart(reason, message)
  }
}
