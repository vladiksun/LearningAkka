package com.vb.chatroom

import akka.actor._


//  https://www.deadcoderising.com/concurrency-made-easy-with-scala-and-akka/
//  https://www.deadcoderising.com/2015-05-26-akka-change-an-actors-behavior-using-context-become/

abstract class Msg

case class Send(msg: String) extends Msg
case class NewMsg(from: String, msg: String) extends Msg
case class Info(msg: String) extends Msg
case class Connect(username: String) extends Msg
case class Broadcast(msg: String) extends Msg
case class Disconnect(msg: String) extends Msg
case object CloseChat extends Msg
case object OpenChat extends Msg


class Server extends Actor {

  override def receive = open(List[(String, ActorRef)]())

  def open(clients: List[(String, ActorRef)]): Receive = {
    case Connect(username) => {
      val newClients = (username, sender) :: clients

      context.watch(sender)
      context.become(open(newClients))

      broadcast(Info(f"$username%s joined the chat"), clients)
    }

    case Broadcast(msg) => {
      val username = getUsername(sender, clients)
      broadcast(NewMsg(username, msg), clients)
    }

    case CloseChat => {
      broadcast(Disconnect("Chatroom is now closed"), clients)
      context.become(closed)
    }

    case Terminated(client) => {
      val username = getUsername(client, clients)
      val newClients = clients.filter(sender != _._2)
      context.become(open(newClients))
      broadcast(Info(f"$username%s left the chat"), newClients)
    }

  }

  def closed: Receive = {
    case OpenChat => context.become(open(List[(String, ActorRef)]()))
    case _ => sender ! Disconnect("Chatroom is closed")
  }

  def broadcast(msg: Msg, clients: List[(String, ActorRef)]) {
    clients.foreach(_._2 ! msg)
  }

  def getUsername(actor: ActorRef, clients: List[(String, ActorRef)]): String = {
    clients.filter(actor == _._2).head._1
  }

}


class Client(val username: String, server: ActorRef) extends Actor {

  server ! Connect(username)

  def receive = {
    case NewMsg(from, msg) => {
      println(f"[$username%s's client] - $from%s: $msg%s")
    }

    case Send(msg) => server ! Broadcast(msg)

    case Info(msg) => {
      println(f"[$username%s's client] - $msg%s")
    }

    case Disconnect => {
      self ! PoisonPill
    }
  }
}
