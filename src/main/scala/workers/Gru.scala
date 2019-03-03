package workers

import akka.actor._
import protocol.Protocol._
import utils.Monitor

/**
  * Created by kayvan on 5/2/16.
  */

class Gru(
  kclMinion: ActorRef,
  geoComputationMinion: ActorRef,
  probe: Option[ActorRef] = None)
  extends Actor {

  def receive: Receive = {

    case event: InValidEndoEvent =>
      kclMinion ! PublishInValidEndoRequest(event)
      Monitor.invalid

    case event: NotValidatedEndoEvent =>
      kclMinion ! event
      Monitor.batch

    case event: ValidEndoEvent =>
      geoComputationMinion ! GeoComputationRequest(event.endoEvent, kclMinion)
      Monitor.valid

    case GeoIpProcessorRestartRequest() =>
      geoComputationMinion ! GeoIpProcessorRestartRequest()
      Monitor.geoipDownload
  
    case  e: MxMinDbDownloadFailure  => 
      Monitor.geoipDownloadFailure
  }

}

