package com.vb.first_step.ex_3_default_failure

import akka.actor.ActorSystem

object TestFail extends App {

	val system = ActorSystem("testSystem")

	val supervisingActor = system.actorOf(SupervisingActor.props, "supervising-actor")

	supervisingActor ! "failChild"

	// parent actor restarted and we can send a message again
	supervisingActor ! "failChild"
}
