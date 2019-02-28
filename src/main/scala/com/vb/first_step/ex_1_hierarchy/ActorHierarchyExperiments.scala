package com.vb.first_step.ex_1_hierarchy

import akka.actor.{ Actor, Props, ActorSystem }
import scala.io.StdIn

object PrintMyActorRefActor {

	def props: Props =
		Props(new PrintMyActorRefActor)
}

class PrintMyActorRefActor extends Actor {

	override def receive: Receive = {
		case "printit" ⇒
			val secondRef = context.actorOf(Props.empty, "second-actor")
			println(s"Second: $secondRef")
	}
}

object ActorHierarchyExperiments extends App {
	val system = ActorSystem("testSystem")

	val firstRef = system.actorOf(PrintMyActorRefActor.props, "first-actor")
	println(s"First: $firstRef")
	firstRef ! "printit"

	println(">>> Press ENTER to exit <<<")

	try
		StdIn.readLine()
	finally
		system.terminate()
}