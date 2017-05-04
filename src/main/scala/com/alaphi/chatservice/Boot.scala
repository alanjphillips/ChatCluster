package com.alaphi.chatservice

import akka.actor.{ActorRef, ActorSystem}

object Boot extends App {

  val system = ActorSystem("ChatService")

}
