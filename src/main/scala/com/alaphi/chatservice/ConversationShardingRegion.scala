package com.alaphi.chatservice

import akka.actor.{ActorRef, ActorSystem}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings, ShardRegion}

object ConversationShardingRegion {

  def start(system: ActorSystem, imForwarder: InstantMessageForwarder, numberOfShards: Int): ActorRef = {
    ClusterSharding(system).start(
      typeName = "ConversationShardingRegion",
      entityProps = ConversationActor.props(imForwarder),
      settings = ClusterShardingSettings(system),
      extractEntityId = idExtractor,
      extractShardId = shardResolver(numberOfShards))
  }

  def idExtractor: ShardRegion.ExtractEntityId = {
    case msg: TextMessage => (msg.conversationKey, msg)
    case req: GetLatestChatter => (req.conversationKey, req)
  }

  def shardResolver(numberOfShards: Int): ShardRegion.ExtractShardId = {
    case msg: TextMessage => (math.abs(msg.conversationKey.hashCode) % numberOfShards).toString
    case req: GetLatestChatter => (math.abs(req.conversationKey.hashCode) % numberOfShards).toString
  }

}
