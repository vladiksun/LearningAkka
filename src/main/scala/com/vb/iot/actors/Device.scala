package com.vb.iot.actors

import akka.actor.{ Actor, ActorLogging, Props }

object Device {
	def props(groupId: String, deviceId: String): Props = Props(new Device(groupId, deviceId))

	// protocol for writing temperature
	final case class RecordTemperatureRequest(requestId: Long, value: Double)
	final case class TemperatureRecordedResponse(requestId: Long)

	// protocol for reading temperature
	final case class ReadTemperatureRequest(requestId: Long)
	final case class TemperatureResponse(requestId: Long, value: Option[Double])
}

class Device(groupId: String, deviceId: String) extends Actor with ActorLogging {
	import Device._

	var lastTemperatureReading: Option[Double] = None

	override def preStart(): Unit = log.info("Device actor {}-{} started", groupId, deviceId)

	override def postStop(): Unit = log.info("Device actor {}-{} stopped", groupId, deviceId)

	override def receive: Receive = {
		case DeviceManager.TrackDeviceRequest(`groupId`, `deviceId`) ⇒
			sender() ! DeviceManager.DeviceRegisteredResponse

		case DeviceManager.TrackDeviceRequest(groupId, deviceId) ⇒
			log.warning(
				"Ignoring TrackDevice request for {}-{}.This actor is responsible for {}-{}.",
				groupId, deviceId, this.groupId, this.deviceId
			)

		case RecordTemperatureRequest(id, value) ⇒
			log.info("Recorded temperature reading {} with {}", value, id)
			lastTemperatureReading = Some(value)
			sender() ! TemperatureRecordedResponse(id)

		case ReadTemperatureRequest(id) ⇒
			sender() ! TemperatureResponse(id, lastTemperatureReading)
	}

}
