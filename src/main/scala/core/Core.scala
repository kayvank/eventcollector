package core

/**
  * Created by kayvan.kazeminejad@vevo.com on 12/8/15.
  */

import akka.actor.{ActorSystem, Props}
import api._
import workers.{EndoProcessingSupervisor}

/**
  * @note mix this trade & override with BootedCore at runtime or test-kit at test
  */
trait Core {
  implicit def system: ActorSystem
}

/**
  * @note mix this trade for run time only. In test use the test-kit actor-system
  */
trait BootedCore extends Core {
  implicit lazy val system = ActorSystem("event-collector")
}

/**
  * Impl of Core are required. This will allow us to test test-kit
  * these are the highest level actors suppoervised by akka system.gaurdian
  */
trait CoreActors {
  this: Core =>

  val apiRouter = system.actorOf(Props[ApiRouter], "master")

  val endoProcessingSpervisor =
    system.actorOf(Props(classOf[EndoProcessingSupervisor],
      apiRouter,
      None),
      "endoProcessingSpervisor")
}
