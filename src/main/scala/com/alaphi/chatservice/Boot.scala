package com.alaphi.chatservice

import akka.actor.{ActorRef, ActorSystem}

object Boot extends App {

  val system = ActorSystem("ChatService")

  val chatRegion: ActorRef = ConversationShardingRegion.start(system, 10)

  val messageConsumer = system.actorOf(MessageConsumerActor.props(chatRegion))
}
