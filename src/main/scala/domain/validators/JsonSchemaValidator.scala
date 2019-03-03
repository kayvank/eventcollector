package domain.validators

import com.eclipsesource.schema._
import domain.Domain.EndoEvent
import play.api.data.validation.ValidationError
import play.api.libs.json._

import scala.io.Source

object EndoSchemaValidator {

  case class ValidationException(errMsg: JsObject) extends Exception {
  }

  import utils.Global._

  private val schema = Json.parse(
    Source.fromInputStream(getClass.getResourceAsStream(
      cfgVevo.getString("endo.schema.path"))).mkString).asOpt[JsObject]
  require(schema.isDefined)

  val validate: EndoEvent => Either[ValidationException, EndoEvent] = endoE =>
    SchemaValidator.validate(Json.fromJson[SchemaType](schema.get).get,
      endoE.eventList).asEither.fold(
      l => Left(ValidationException(errMsg = Json.obj(
        "key" -> "invalid-payload",
        "message" -> "Message format not recognized, message rejected",
        "extra" -> Json.obj(
          "errors" -> asJ(l),
          "payload" -> endoE.eventList)))),
      r => Right(endoE))

  def asJ (s: Seq[(JsPath, Seq[ValidationError]) ]) = {
    val keys = s.flatMap(_._1.path.map(_.toJsonString)).toString
    val values = s.flatMap(_._2.map( x => x.message))
    Json.obj(keys -> values)

  }

}
