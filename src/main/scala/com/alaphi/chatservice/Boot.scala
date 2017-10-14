package com.alaphi.chatservice

import akka.actor.{ActorRef, ActorSystem}

object Boot extends App {

  implicit val system = ActorSystem("ChatService")
  implicit val executionContext = system.dispatcher

  val imSender = KafkaPublisher("instant_message_out", numPartitions = 3)

  val blockSender = KafkaPublisher("latest_messages_block", numPartitions = 3)

  val chatRegion: ActorRef = ConversationShardingRegion.start(imSender, blockSender, numberOfShards = 30)

  InstantMessageConsumer(chatRegion).start
}
