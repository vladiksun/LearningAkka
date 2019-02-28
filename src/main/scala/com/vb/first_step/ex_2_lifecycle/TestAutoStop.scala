package com.vb.first_step.ex_2_lifecycle

import akka.actor.ActorSystem

object TestAutoStop extends App {

	val system = ActorSystem("testSystem")

	val first = system.actorOf(StartStopActor1.props, "first")
	first ! "stop"

}
