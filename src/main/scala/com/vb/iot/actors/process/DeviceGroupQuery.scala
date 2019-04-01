package com.vb.iot.actors.process

import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, Props, Terminated}
import com.vb.iot.actors.domain.{Device, DeviceGroup}

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
      deviceActor ! Device.ReadTemperature(0)
    }
  }

  override def postStop(): Unit = {
    queryTimeoutTimer.cancel()
  }


  override def receive: Receive =
    waitingForReplies(
      Map.empty,
      actorToDeviceId.keySet
    )

  //  One important thing to note is that the function waitingForReplies does not handle the messages directly.
  //  It returns a Receive function that will handle the messages.
  def waitingForReplies(
                         repliesSoFar: Map[String, DeviceGroup.TemperatureReading],
                         stillWaiting: Set[ActorRef]
                       ): Receive = {
    // the device responded
    case Device.RespondTemperature(0, valueOption) ⇒
      val deviceActor = sender()
      val reading = valueOption match {
        case Some(value) ⇒ DeviceGroup.Temperature(value)
        case None ⇒ DeviceGroup.TemperatureNotAvailable
      }
      receivedResponse(deviceActor, reading, stillWaiting, repliesSoFar)

    // device terminated
    case Terminated(deviceActor) ⇒
      receivedResponse(deviceActor, DeviceGroup.DeviceNotAvailable, stillWaiting, repliesSoFar)

    case CollectionTimeout ⇒
      val timedOutReplies =
        stillWaiting.map { deviceActor ⇒
          val deviceId = actorToDeviceId(deviceActor)
          deviceId -> DeviceGroup.DeviceTimedOut
        }
      requester ! DeviceGroup.RespondAllTemperatures(requestId, repliesSoFar ++ timedOutReplies)
      context.stop(self)
  }


  def receivedResponse(
                        deviceActor: ActorRef,
                        reading: DeviceGroup.TemperatureReading,
                        stillWaiting: Set[ActorRef],
                        repliesSoFar: Map[String, DeviceGroup.TemperatureReading]
                      ): Unit = {

    context.unwatch(deviceActor)

    val deviceId = actorToDeviceId(deviceActor)
    val newStillWaiting = stillWaiting - deviceActor

    val newRepliesSoFar = repliesSoFar + (deviceId -> reading)

    if (newStillWaiting.isEmpty) {
      requester ! DeviceGroup.RespondAllTemperatures(requestId, newRepliesSoFar)
      context.stop(self)
    } else {
      // recursive call
      context.become(waitingForReplies(newRepliesSoFar, newStillWaiting))
    }
  }
}