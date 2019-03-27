import akka.actor.{ActorSystem, PoisonPill}
import akka.testkit.{TestKit, TestProbe}
import com.vb.iot.actors.domain.{Device, DeviceGroup, DeviceManager}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._


class DeviceSpec(_system: ActorSystem)
		extends TestKit(_system)
				with Matchers
				with WordSpecLike
				with BeforeAndAfterAll {


	def this() = this(ActorSystem("AkkaQuickstartSpec"))

	override def afterAll: Unit = {
		shutdown(system)
	}

	"reply with empty reading if no temperature is known" in {
		val probe = TestProbe()
		val deviceActor = system.actorOf(Device.props("group", "device"))

		deviceActor.tell(Device.ReadTemperatureRequest(requestId = 42), probe.ref)

		val response = probe.expectMsgType[Device.TemperatureResponse]

		response.requestId should ===(42L)
		response.value should ===(None)
	}


	"reply with latest temperature reading" in {
		val probe = TestProbe()
		val deviceActor = system.actorOf(Device.props("group", "device"))

		deviceActor.tell(Device.RecordTemperatureRequest(requestId = 1, 24.0), probe.ref)
		probe.expectMsg(Device.TemperatureRecordedResponse(requestId = 1))

		deviceActor.tell(Device.ReadTemperatureRequest(requestId = 2), probe.ref)
		val response1 = probe.expectMsgType[Device.TemperatureResponse]
		response1.requestId should ===(2L)
		response1.value should ===(Some(24.0))

		deviceActor.tell(Device.RecordTemperatureRequest(requestId = 3, 55.0), probe.ref)
		probe.expectMsg(Device.TemperatureRecordedResponse(requestId = 3))

		deviceActor.tell(Device.ReadTemperatureRequest(requestId = 4), probe.ref)
		val response2 = probe.expectMsgType[Device.TemperatureResponse]
		response2.requestId should ===(4L)
		response2.value should ===(Some(55.0))
	}

	"reply to registration requests" in {
		val probe = TestProbe()
		val deviceActor = system.actorOf(Device.props("group", "device"))

		deviceActor.tell(DeviceManager.TrackDeviceRequest("group", "device"), probe.ref)
		probe.expectMsg(DeviceManager.DeviceRegisteredResponse)
		probe.lastSender should ===(deviceActor)
	}

	"ignore wrong registration requests" in {
		val probe = TestProbe()
		val deviceActor = system.actorOf(Device.props("group", "device"))

		deviceActor.tell(DeviceManager.TrackDeviceRequest("wrongGroup", "device"), probe.ref)
		probe.expectNoMessage(500.milliseconds)

		deviceActor.tell(DeviceManager.TrackDeviceRequest("group", "Wrongdevice"), probe.ref)
		probe.expectNoMessage(500.milliseconds)
	}

	"be able to register a device actor" in {
		val probe = TestProbe()
		val groupActor = system.actorOf(DeviceGroup.props("group"))

		groupActor.tell(DeviceManager.TrackDeviceRequest("group", "device1"), probe.ref)
		probe.expectMsg(DeviceManager.DeviceRegisteredResponse)
		val deviceActor1 = probe.lastSender

		groupActor.tell(DeviceManager.TrackDeviceRequest("group", "device2"), probe.ref)
		probe.expectMsg(DeviceManager.DeviceRegisteredResponse)
		val deviceActor2 = probe.lastSender
		deviceActor1 should !==(deviceActor2)

		// Check that the device actors are working
		deviceActor1.tell(Device.RecordTemperatureRequest(requestId = 0, 1.0), probe.ref)
		probe.expectMsg(Device.TemperatureRecordedResponse(requestId = 0))
		deviceActor2.tell(Device.RecordTemperatureRequest(requestId = 1, 2.0), probe.ref)
		probe.expectMsg(Device.TemperatureRecordedResponse(requestId = 1))
	}

	"ignore requests for wrong groupId" in {
		val probe = TestProbe()
		val groupActor = system.actorOf(DeviceGroup.props("group"))

		groupActor.tell(DeviceManager.TrackDeviceRequest("wrongGroup", "device1"), probe.ref)
		probe.expectNoMessage(500.milliseconds)
	}

	"return same actor for same deviceId" in {
		val probe = TestProbe()
		val groupActor = system.actorOf(DeviceGroup.props("group"))

		groupActor.tell(DeviceManager.TrackDeviceRequest("group", "device1"), probe.ref)
		probe.expectMsg(DeviceManager.DeviceRegisteredResponse)
		val deviceActor1 = probe.lastSender

		groupActor.tell(DeviceManager.TrackDeviceRequest("group", "device1"), probe.ref)
		probe.expectMsg(DeviceManager.DeviceRegisteredResponse)
		val deviceActor2 = probe.lastSender

		deviceActor1 should ===(deviceActor2)
	}

	"be able to list active devices" in {
		val probe = TestProbe()
		val groupActor = system.actorOf(DeviceGroup.props("group"))

		groupActor.tell(DeviceManager.TrackDeviceRequest("group", "device1"), probe.ref)
		probe.expectMsg(DeviceManager.DeviceRegisteredResponse)

		groupActor.tell(DeviceManager.TrackDeviceRequest("group", "device2"), probe.ref)
		probe.expectMsg(DeviceManager.DeviceRegisteredResponse)

		groupActor.tell(DeviceGroup.DeviceListRequest(requestId = 0), probe.ref)
		probe.expectMsg(DeviceGroup.ReplyDeviceListResponse(requestId = 0, Set("device1", "device2")))
	}

	"be able to list active devices after one shuts down" in {
		val probe = TestProbe()
		val groupActor = system.actorOf(DeviceGroup.props("group"))

		groupActor.tell(DeviceManager.TrackDeviceRequest("group", "device1"), probe.ref)
		probe.expectMsg(DeviceManager.DeviceRegisteredResponse)
		val toShutDown = probe.lastSender // this is a device actor

		groupActor.tell(DeviceManager.TrackDeviceRequest("group", "device2"), probe.ref)
		probe.expectMsg(DeviceManager.DeviceRegisteredResponse)

		groupActor.tell(DeviceGroup.DeviceListRequest(requestId = 0), probe.ref)
		probe.expectMsg(DeviceGroup.ReplyDeviceListResponse(requestId = 0, Set("device1", "device2")))

		probe.watch(toShutDown)
		toShutDown ! PoisonPill
		probe.expectTerminated(toShutDown)

		// using awaitAssert to retry because it might take longer for the groupActor
		// to see the Terminated, that order is undefined
		probe.awaitAssert {
			groupActor.tell(DeviceGroup.DeviceListRequest(requestId = 1), probe.ref)
			probe.expectMsg(DeviceGroup.ReplyDeviceListResponse(requestId = 1, Set("device2")))
		}
	}
}
