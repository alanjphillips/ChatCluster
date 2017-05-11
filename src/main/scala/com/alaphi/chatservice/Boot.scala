package com.alaphi.chatservice

import akka.actor.{ActorRef, ActorSystem}
import akka.stream.ActorMaterializer

object Boot extends App {

  implicit val system = ActorSystem("ChatService")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val imForwarder = InstantMessageForwarder(numPartitions = 3)

  val chatRegion: ActorRef = ConversationShardingRegion.start(imForwarder, numberOfShards = 30)

  InstantMessageConsumer(chatRegion).start
}
