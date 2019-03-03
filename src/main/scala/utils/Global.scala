package utils

import com.typesafe.config.ConfigFactory

/**
  * Created by v644164 on 5/19/15.
  */
object Global {

  val cfg = ConfigFactory.load
  val cfgVevo = cfg.getConfig("vevo")

  final val allEndoEvents="allEndoEvents"
  final val endoValidEvents="endoValidEvents"
  final val endoValidEventsPublicStream="endoValidEventsPublic"
  final val endoInvalidEvents="endoInvalidEvents"
}
