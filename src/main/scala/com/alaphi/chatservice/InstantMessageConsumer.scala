package com.alaphi.chatservice

import akka.actor.{ActorRef, ActorSystem}
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.scaladsl.Sink
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import io.circe.Json
import io.circe.parser.{decode, parse}
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}

import com.alaphi.chatservice.Message._

class InstantMessageConsumer(chatRegion: ActorRef)(implicit system: ActorSystem) {

  val decider: Supervision.Decider = {
    case _  => Supervision.Restart
  }

  implicit val materializer = ActorMaterializer(
    ActorMaterializerSettings(system).withSupervisionStrategy(decider))

  def start(): Unit = {
    val consumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new StringDeserializer)
      .withBootstrapServers("kafka-1:9092,kafka-2:9093,kafka-3:9094")
      .withGroupId("chat-group")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

    val topics = Subscriptions.topics("instant_message_in", "latest_messages_request")

    Consumer.committableSource(consumerSettings, topics)
      .map { msg =>
        val json = parse(msg.record.value()).getOrElse(Json.Null).noSpaces
        val cmdE = decode[Command](json)
        cmdE map (cmd => chatRegion ! cmd)
        msg
      }
      .mapAsync(1) { msg =>
        msg.committableOffset.commitScaladsl()
      }
      .runWith(Sink.ignore)
  }

}

object InstantMessageConsumer {
  def apply(chatRegion: ActorRef)(implicit system: ActorSystem): InstantMessageConsumer = new InstantMessageConsumer(chatRegion)
}
