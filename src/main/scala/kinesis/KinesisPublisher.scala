package kinesis

import java.nio.ByteBuffer
import com.typesafe.scalalogging.LazyLogging
import io.github.cloudify.scala.aws.kinesis.Client
import io.github.cloudify.scala.aws.kinesis.Client.ImplicitExecution._
import io.github.cloudify.scala.aws.kinesis.Definitions.{PutResult, PutsResult}
import scala.concurrent.Future
import scala.language.implicitConversions
import scala.concurrent.ExecutionContext.Implicits.global
import utils.Global._
import com.amazonaws.auth._
/**
  * Created by Kayvan Kazeminejad on 1/13/16.
  */

trait KinesisPublisher {
  this: LazyLogging =>

  import io.github.cloudify.scala.aws.kinesis.KinesisDsl._

  object KinesisConfig {

    import utils.Global._

    implicit val kinesis =
      Client.fromCredentials(new InstanceProfileCredentialsProvider() )
        //
        // NOTE for local run, set env vars accourdingly & uncommend line.
        //
        // Client.fromCredentials(cfgVevo.getString("aws.access-key"), cfgVevo.getString("aws.secret-key"))

    logger.info(s"aws-access-key= ${cfgVevo.getString("aws.access-key")}")
    val KCLBATCHSIZE = cfgVevo.getInt("kinesis.batch.size")
  }

  import KinesisConfig._
  import KCLType._

  val kinesisStreams = Map(
    allEndoEvents ->
      Kinesis.stream(cfgVevo.getString("kinesis.stream.name.endo.all")),
    endoValidEvents ->
      Kinesis.stream(cfgVevo.getString("kinesis.stream.name.endo.valid")),
    endoValidEventsPublicStream ->
      Kinesis.stream(cfgVevo.getString("kinesis.stream.name.endo.public")),
    endoInvalidEvents ->
      Kinesis.stream(cfgVevo.getString("kinesis.stream.name.endo.invalid"))
  )

  val publishList: String => List[KCLData] => Future[List[String]] = streamName => events => {
    val ret: Future[List[ PutsResult] ] = {
      val eventsList = events.grouped(KCLBATCHSIZE).toList
        Future.sequence(
        eventsList.map( evs =>  {
        val kclPutsResults: Future[PutsResult] = kinesisStreams(streamName).multiPut(evs.map(e => (e.value, e.key)))
          kclPutsResults
      }))
    }
    ret.map( x => x.map(  r => r.sequenceNumber).flatten) recover {
      case e: Exception =>
        logger.error(s"kcl-publish failed with error = ${e.getMessage}")
        List(e.getMessage)
    }
  }

  val publish: String => KCLData => Future[String] = streamName => event => {
    val ret: Future[PutResult] =
      kinesisStreams(streamName).put(event.value, event.key)
    ret.map(r => r.sequenceNumber).recover {
      case e: Exception =>
        logger.error(s"kcl-publish failed with error = ${e.getMessage}")
        e.printStackTrace()
        e.getMessage
    }
  }
}

object KinesisPublisher extends KinesisPublisher
  with LazyLogging
