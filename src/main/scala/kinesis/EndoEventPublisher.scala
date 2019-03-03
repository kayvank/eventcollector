package kinesis


import domain.Domain.EndoEventWithGeo
import domain.transformers.DomainJsonFormatters
import protocol.Protocol.{InValidEndoEvent, NotValidatedEndoEvent}

import scala.concurrent.Future

/**
  * Created by kayvan on 11/6/16.
  */

object EndoEventPublisher {

  import KCLType._
  import KclTypeConverters._
  import utils.Global._
  import scala.concurrent.ExecutionContext.Implicits.global
  
  def endoEventPublisher(event: NotValidatedEndoEvent)
    (implicit kclPublish: String => KCLData => Future[String]): Future[String] = {
    kclPublish(allEndoEvents)(
      jsonToKcl(DomainJsonFormatters.notValidatedEndoEvent2Json(event)))
  }

  def endoEventPublisher(event: EndoEventWithGeo)
    (implicit kclPublishList: String => List[KCLData] => Future[List[String]]): Future[List[String]] = {

    val kclData2Publish = DomainJsonFormatters.endoWithGeo2jsons(event) map (jsonToKcl)

    val _ = kclPublishList(endoValidEventsPublicStream)(kclData2Publish)

    kclPublishList(endoValidEvents)(kclData2Publish)
  }

  def endoEventPublisher(event: InValidEndoEvent)
    (implicit kclPublish: String => KCLData => Future[String]): Future[String] = {
    kclPublish(endoInvalidEvents)(
      jsonToKcl(
        DomainJsonFormatters.inValidEndoEvent2Json(event))
    )
  }
}
