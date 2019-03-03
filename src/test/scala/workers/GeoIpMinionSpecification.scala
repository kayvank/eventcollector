package workers

import akka.actor._
import akka.testkit._
import com.typesafe.scalalogging.LazyLogging
import domain.Domain.EndoEvent
import org.specs2.mutable.SpecificationLike
import play.api.libs.json.{JsObject, Json}
import protocol._

import scala.concurrent.duration.{Duration, _}
import scala.io.Source

/**
  * Created by kayvan on 11/5/16.
  */
class GeoIpMinionSpecification extends TestKit(ActorSystem("KclWorkerSpec-system_01"))
  with ImplicitSender
  with SpecificationLike
  with LazyLogging {

  val payload = Json.parse(
    Source.fromInputStream(getClass.getResourceAsStream("/event-data.json")).mkString
  ).asOpt[JsObject]


  "GeoIp-computation-minion Specifications".title

//  "geoIp-Minion will compute the GeoLocation of valid IP address" >> {
//    val probe = TestProbe()
//    val minionProxyTest =
//      system.actorOf(Props(classOf[GeoComputationMinionProxy], Some(probe.ref)),
//        "geoComputationMinionProxyTest")
//
//    val endoEvent = EndoEvent(
//      eventList = payload.get,
//      ip = Some("204.28.115.197"),
//      httpUserAgent = Some("test-browser"))
//    minionProxyTest ! Protocol.GeoComputationRequest(endoEvent)
//
//    expectMsgPF(Duration("3 second")) {
//      case res: Protocol.GeoComputationResponse =>
//        true
//      case e =>
//        logger.info(s"not sure what I got ${e}")
//        false
//    }
//  }
  "geoip minion restarts his minion worker " >> {
    val probe = TestProbe()
    val geoComputationminionProxyTest =
      system.actorOf(Props(classOf[GeoComputationMinionProxy], Some(probe.ref)),
        "geoComputationMinionProxyTest-002")
    val kclTest = TestProbe("kcl-test")
    val monitoringTest = TestProbe("monitoringTest")
    val gru = system.actorOf(Props(
      classOf[Gru],
      kclTest.ref,
      geoComputationminionProxyTest,
      None),
      "gru-test")

    gru ! Protocol.GeoIpProcessorRestartRequest()
    probe.expectMsgPF(Duration("10 second")) {
      case Protocol.GeoIpProcessorRestartRequest() =>
        true
      case e =>
        logger.info(s"expected GeoIpProcessorRestartRequest. got --> ${e}")
        false
    }
  }
}
