package com.vb.iot.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import com.vb.iot.actors.DeviceManager.TrackDeviceRequest

object DeviceManager {
	def props(): Props = Props(new DeviceManager)

	final case class TrackDeviceRequest(groupId: String, deviceId: String)
	case object DeviceRegisteredResponse
}

class DeviceManager extends Actor with ActorLogging {

	var groupIdToItsActor = Map.empty[String, ActorRef]
	var actorToGroupId = Map.empty[ActorRef, String]

	override def preStart(): Unit = log.info("DeviceManager started")

	override def postStop(): Unit = log.info("DeviceManager stopped")

	override def receive = {
		case trackMsg @ TrackDeviceRequest(groupId, _) ⇒
			groupIdToItsActor.get(groupId) match {
				case Some(ref) ⇒
					ref forward trackMsg // forward to the group actor
				case None ⇒
					log.info("Creating device group actor for {}", groupId)
					val groupActor = context.actorOf(DeviceGroup.props(groupId), "group-" + groupId)

					context.watch(groupActor)

					groupActor forward trackMsg

					groupIdToItsActor += groupId -> groupActor
					actorToGroupId += groupActor -> groupId
			}

		case Terminated(groupActor) ⇒
			val groupId = actorToGroupId(groupActor)
			log.info("Device group actor for {} has been terminated", groupId)
			actorToGroupId -= groupActor
			groupIdToItsActor -= groupId

	}

}