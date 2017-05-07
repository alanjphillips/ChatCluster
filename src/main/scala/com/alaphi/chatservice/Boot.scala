package com.alaphi.chatservice

import akka.actor.{ActorRef, ActorSystem}

object Boot extends App {

  val system = ActorSystem("ChatService")

  val imForwarder = InstantMessageForwarder()

  val chatRegion: ActorRef = ConversationShardingRegion.start(system, imForwarder, 10)

  val messageConsumer = system.actorOf(MessageConsumerActor.props(chatRegion))
}
