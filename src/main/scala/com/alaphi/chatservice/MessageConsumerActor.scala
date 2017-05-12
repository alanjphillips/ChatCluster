package com.alaphi.chatservice

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.Materializer
import akka.stream.scaladsl.Sink
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}
import scala.concurrent.Future
import io.circe._
import io.circe.parser._

import com.alaphi.chatservice.Message._

class MessageConsumerActor(chatRegion: ActorRef)(implicit mat: Materializer) extends Actor with ActorLogging {
  implicit val system = context.system

  override def receive: Receive = Actor.emptyBehavior

  override def preStart(): Unit = {
    val consumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new StringDeserializer)
      .withBootstrapServers("kafka-1:9092,kafka-2:9093,kafka-3:9094")
      .withGroupId("chat-group")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

    val done =
      Consumer.committableSource(consumerSettings, Subscriptions.topics("instant_message_in", "latest_messages_request"))
        .mapAsync(1) { msg =>
          val json: Json = parse(msg.record.value()).getOrElse(Json.Null)
          val cmdE =  decode[Command](json.noSpaces)
          cmdE map (cmd => chatRegion ! cmd)
          Future.successful(msg)
        }
        .mapAsync(1) { msg =>
          msg.committableOffset.commitScaladsl()
        }
        .runWith(Sink.ignore)
  }

}

object MessageConsumerActor {
   def props(chatRegion: ActorRef)(implicit mat: Materializer) : Props = {
     Props(new MessageConsumerActor(chatRegion))
   }
}
