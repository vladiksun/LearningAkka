package com.vb.iot.actors.domain

import akka.actor.{Actor, ActorLogging, ActorRef, Props, Terminated}
import com.vb.iot.actors.domain.DeviceGroup.{DeviceListRequest, ReplyDeviceListResponse, RequestAllTemperatures}
import com.vb.iot.actors.domain.DeviceManager.RequestTrackDevice
import com.vb.iot.actors.process.DeviceGroupQuery
import scala.concurrent.duration._

// Represent domain object
object DeviceGroup {
	def props(groupId: String): Props = Props(new DeviceGroup(groupId))

	final case class DeviceListRequest(requestId: Long)
	final case class ReplyDeviceListResponse(requestId: Long, ids: Set[String])

	final case class RequestAllTemperatures(requestId: Long)
	final case class RespondAllTemperatures(requestId: Long, temperatures: Map[String, TemperatureReading])

	sealed trait TemperatureReading
	final case class Temperature(value: Double) extends TemperatureReading
	case object TemperatureNotAvailable extends TemperatureReading
	case object DeviceNotAvailable extends TemperatureReading
	case object DeviceTimedOut extends TemperatureReading

}

class DeviceGroup(groupId: String) extends Actor with ActorLogging {

	// child device actors
	var deviceIdToItsActor = Map.empty[String, ActorRef]

	var actorToDeviceId = Map.empty[ActorRef, String]

	override def preStart(): Unit = log.info("DeviceGroup {} started", groupId)

	override def postStop(): Unit = log.info("DeviceGroup {} stopped", groupId)

	override def receive: Receive = {
		// You only include the @ when you want to also deal with the object itself
		// constructor pattern matching + the object itself
		case trackMsg @ RequestTrackDevice(`groupId`, _) ⇒ // this is The literal definition of identifiers
			deviceIdToItsActor.get(trackMsg.deviceId) match { // try to find the device in the cache
				case Some(deviceActor) ⇒
					deviceActor forward trackMsg // if found then forward the message to it
				case None ⇒ // if not found then create one and put it to the cache
					log.info("Creating device actor for {}", trackMsg.deviceId)
					val deviceActor = context.actorOf(Device.props(groupId, trackMsg.deviceId), s"device-${trackMsg.deviceId}")

					// Death Watch feature that allows an actor to watch another actor and be notified if the other actor is stopped
					context.watch(deviceActor)

					actorToDeviceId += deviceActor -> trackMsg.deviceId
					deviceIdToItsActor += trackMsg.deviceId -> deviceActor

					deviceActor forward trackMsg
			}

		case RequestTrackDevice(groupId, deviceId) ⇒
			log.warning(
				"Ignoring TrackDevice request for {}. This actor is responsible for {}.",
				groupId, this.groupId
			)

		case DeviceListRequest(requestId) ⇒
			sender() ! ReplyDeviceListResponse(requestId, deviceIdToItsActor.keySet)

		// Response to Death Watch feature
		case Terminated(deviceActor) ⇒
			val deviceId = actorToDeviceId(deviceActor)
			log.info("Device actor for {} has been terminated", deviceId)
			actorToDeviceId -= deviceActor
			deviceIdToItsActor -= deviceId

		case RequestAllTemperatures(requestId) ⇒
			context.actorOf(DeviceGroupQuery.props(
				actorToDeviceId = actorToDeviceId,
				requestId = requestId,
				requester = sender(),
				3.seconds
			))

	}
}