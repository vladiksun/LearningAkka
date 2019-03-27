package com.vb.iot.actors.process

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props}
import com.vb.iot.actors.domain.Device

import scala.concurrent.duration.FiniteDuration


//    https://doc.akka.io/docs/akka/current/guide/tutorial_5.html
object DeviceGroupQuery {

  case object CollectionTimeout

  def props(
             actorToDeviceId: Map[ActorRef, String],
             requestId: Long,
             requester: ActorRef,
             timeout: FiniteDuration
           ): Props = {
    Props(new DeviceGroupQuery(actorToDeviceId, requestId, requester, timeout))
  }
}


class DeviceGroupQuery(
                        actorToDeviceId: Map[ActorRef, String],
                        requestId: Long,
                        requester: ActorRef,
                        timeout: FiniteDuration
                      ) extends Actor with ActorLogging {

  import DeviceGroupQuery._
  import context.dispatcher

  val queryTimeoutTimer: Cancellable = context.system.scheduler.scheduleOnce(timeout, self, CollectionTimeout)

  override def preStart(): Unit = {
    actorToDeviceId.keysIterator.foreach { deviceActor ⇒
      // watch every device actor to be able to find out if it was terminated
      context.watch(deviceActor)
      deviceActor ! Device.ReadTemperatureRequest(0)
    }
  }

  override def postStop(): Unit = {
    queryTimeoutTimer.cancel()
  }


  override def receive: Receive = ???
}