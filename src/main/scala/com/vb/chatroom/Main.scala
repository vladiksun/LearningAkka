package com.vb.chatroom

import akka.actor._

object Main extends App {

  override def main(args: Array[String]) {
    val system = ActorSystem("System")

    val server = system.actorOf(Props[Server])

    val sam = system.actorOf(Props(new Client("Sam", server)))
    sam ! Send("Hi, anyone here?")

    val mia = system.actorOf(Props(new Client("Mia", server)))
    val luke = system.actorOf(Props(new Client("Luke", server)))

    mia ! Send("Hello")

    luke ! Disconnect

//    server ! CloseChat
//    system.actorOf(Props(new Client("Mia", server)))
//
//    server ! OpenChat
//    val mia2 = system.actorOf(Props(new Client("Mia", server)))
//    mia2 ! Send("Finally in!")



//    system.terminate()

  }

}
