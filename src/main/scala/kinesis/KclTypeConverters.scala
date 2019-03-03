package kinesis

import java.nio.ByteBuffer
import play.api.libs.json.JsObject

/**
  * Created by kayvan on 11/2/16.
  */

object KCLType {

  case class KCLAsString(key: String, value: String)

  case class KCLData(key: String, value: ByteBuffer)

  implicit def asKclData(k: String, v: ByteBuffer) = KCLData(key = k, value = v)

}

object KclTypeConverters {

  import KCLType._
  import play.api.libs.json._

  val kclPartitionKey: JsObject => String = event =>
  (event \ "device_context" \ "anon_id").asOpt[String].getOrElse(
    (event \ "device_context" \ "user_id").asOpt[String].getOrElse(
        scala.util.Random.nextInt.toString))

  val json2string: JsObject => KCLAsString =
    json => KCLAsString(kclPartitionKey(json), Json.stringify(json))
  val string2kcl: KCLAsString => KCLData =
    kclSsString => KCLData(kclSsString.key, ByteBuffer.wrap(kclSsString.value.getBytes()))
  val jsonToKcl: JsObject => KCLData = json2string andThen (string2kcl)

  def keyBuilder(event: JsObject) = {

  }

}
