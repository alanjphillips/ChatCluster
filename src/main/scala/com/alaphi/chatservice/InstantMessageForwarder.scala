package com.alaphi.chatservice

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.Materializer
import akka.stream.scaladsl.Source
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}
import io.circe.syntax._

import com.alaphi.chatservice.Message._

import scala.concurrent.{ExecutionContext, Future}


class InstantMessageForwarder(numPartitions: Int = 3)(implicit as: ActorSystem, mat: Materializer, ec: ExecutionContext) {

  val producerSettings = ProducerSettings(as, new ByteArraySerializer, new StringSerializer)
    .withBootstrapServers("kafka-1:9092,kafka-2:9093,kafka-3:9094")

  val kafkaProducer = producerSettings.createKafkaProducer()

  def deliverMessage(message: MessageEvent) : Future[Done] = {
    val done = Source.single(message)
      .map { msg =>
        val partition = msg.conversationKey.hashCode % numPartitions
        val messageEventJson = msg.asJson.noSpaces
        new ProducerRecord[Array[Byte], String]("instant_message_out", partition, null, messageEventJson)
      }
      .runWith(Producer.plainSink(producerSettings, kafkaProducer))

    done
  }

  def deliverLatestChat(chatMessages: LatestChatter) : Future[Done] = {
    val done = Source.single(chatMessages)
      .map { msg =>
        val partition = msg.conversationKey.hashCode % numPartitions
        val latestChatterJson = msg.asJson.noSpaces
        new ProducerRecord[Array[Byte], String]("latest_message_block", partition, null, latestChatterJson)
      }
      .runWith(Producer.plainSink(producerSettings, kafkaProducer))

    done
  }

}

object InstantMessageForwarder {
  def apply(numPartitions: Int)(implicit as: ActorSystem, mat: Materializer, ec: ExecutionContext): InstantMessageForwarder = new InstantMessageForwarder(numPartitions)
}