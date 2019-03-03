package api

import akka.io.IO
import spray.can.Http
import core.{Core, CoreActors}
import utils.Global._

/**
  * Created by kayvan.kazeminejad@vevo.com on 12/8/15.
  */
trait Web {
  this: CoreActors with Core =>
  IO(Http) ! Http.Bind(apiRouter, interface = "0.0.0.0", port = cfgVevo.getInt("api.port"))
}
