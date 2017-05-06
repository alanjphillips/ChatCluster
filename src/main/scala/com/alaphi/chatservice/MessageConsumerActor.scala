package com.alaphi.chatservice

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.kafka.scaladsl.Consumer
import akka.kafka.{ConsumerSettings, Subscriptions}
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Sink
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.{ByteArrayDeserializer, StringDeserializer}

import scala.concurrent.Future

class MessageConsumerActor(chatRegion : ActorRef) extends Actor with ActorLogging {
  implicit val system = context.system
  implicit val materialiser = ActorMaterializer()

  override def receive: Receive = Actor.emptyBehavior

  override def preStart(): Unit = {
    val consumerSettings = ConsumerSettings(system, new ByteArrayDeserializer, new StringDeserializer)
      .withBootstrapServers("kafka-1:9092,kafka-2:9093,kafka-3:9094")
      .withGroupId("group1")
      .withProperty(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest")

    val done =
      Consumer.committableSource(consumerSettings, Subscriptions.topics("chat_messages"))
        .mapAsync(1) { msg =>
          log.info(s"Received message from kafka: $msg")
          val cKey = ""
          val text = ""
          chatRegion ! TextMessage(cKey, text)
          Future.successful(msg)
        }
        .mapAsync(1) { msg =>
          msg.committableOffset.commitScaladsl()
        }
        .runWith(Sink.ignore)
  }

}

object MessageConsumerActor {
   def props(chatRegion : ActorRef) : Props = {
     Props(new MessageConsumerActor(chatRegion))
   }
}
