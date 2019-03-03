/**
  * Created by kayvan.kazeminejad@vevo.com on 12/8/15.
  */

import api.Web
import core.{BootedCore, Core, CoreActors}
import kamon.Kamon

object BootStrap extends App
  with Core
  with BootedCore
  with CoreActors
  with Web {
  Kamon.start()
}
