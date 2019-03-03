package utils

import play.api.libs.json.{JsObject, Json}

import scala.io.Source

object FileToJson {
  def get(filename: String): JsObject = {
    val io = Source.fromInputStream(getClass.getResourceAsStream(filename))
    val str = io.mkString
    io.close
    Json.parse(str).as[JsObject]
  }
}
