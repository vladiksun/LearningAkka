import akka.actor.ActorSystem
import akka.testkit.{TestKit, TestProbe}
import com.vb.iot.device.Device
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

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

		deviceActor.tell(Device.ReadTemperature(requestId = 42), probe.ref)

		val response = probe.expectMsgType[Device.RespondTemperature]

		response.requestId should ===(42L)
		response.value should ===(None)
	}
}
