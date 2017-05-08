package com.alaphi.chatservice

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}
import io.circe._
import io.circe.syntax._
import io.circe.generic.semiauto._

import scala.concurrent.{ExecutionContext, Future}


class InstantMessageForwarder(numPartitions: Int = 3)(implicit as: ActorSystem, mat: Materializer, ec: ExecutionContext) {

  implicit val messageEventEncoder: Encoder[MessageEvent] = deriveEncoder[MessageEvent]
  implicit val latestChatterEncoder: Encoder[LatestChatter] = deriveEncoder[LatestChatter]

  val producerSettings = ProducerSettings(as, new ByteArraySerializer, new StringSerializer)
    .withBootstrapServers("kafka-1:9092,kafka-2:9093,kafka-3:9094")

  val kafkaProducer = producerSettings.createKafkaProducer()

  def deliverMessage(message: MessageEvent) : Future[Done] = {
    val done = Source.single(message)
      .map { msg =>
        val partition = msg.conversationKey.hashCode % numPartitions
        val messageEventJson = msg.asJson.noSpaces
        new ProducerRecord[Array[Byte], String]("conversation_user_destination", partition, null, messageEventJson)
      }
      .runWith(Producer.plainSink(producerSettings, kafkaProducer))

    done
  }

  def deliverLatestChat(chatMessages: LatestChatter) : Future[Done] = {
    val done = Source.single(chatMessages)
      .map { msg =>
        val partition = msg.conversationKey.hashCode % numPartitions
        val latestChatterJson = msg.asJson.noSpaces
        new ProducerRecord[Array[Byte], String]("conversation_user_latest", partition, null, latestChatterJson)
      }
      .runWith(Producer.plainSink(producerSettings, kafkaProducer))

    done
  }

}

object InstantMessageForwarder {
  def apply(numPartitions: Int)(implicit as: ActorSystem, mat: Materializer, ec: ExecutionContext): InstantMessageForwarder = new InstantMessageForwarder(numPartitions)
}