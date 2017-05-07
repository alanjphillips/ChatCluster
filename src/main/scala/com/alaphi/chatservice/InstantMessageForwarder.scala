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


class InstantMessageForwarder(numPartitions: Int = 10)(implicit as: ActorSystem, mat: Materializer, ec: ExecutionContext) {

  implicit val messageEventEncoder: Encoder[MessageEvent] = deriveEncoder[MessageEvent]

  val producerSettings = ProducerSettings(as, new ByteArraySerializer, new StringSerializer)
    .withBootstrapServers("kafka-1:9092,kafka-2:9093,kafka-3:9094")

  val kafkaProducer = producerSettings.createKafkaProducer()

  def deliverMessage(message: MessageEvent) : Future[Done] = {
    val done = Source.single(message)
      .map { msg =>
        val partition = msg.conversationKey.hashCode % numPartitions
        val encodedJson = msg.asJson.noSpaces
        new ProducerRecord[Array[Byte], String]("conversation_user_destination", partition, null, encodedJson)
      }
      .runWith(Producer.plainSink(producerSettings, kafkaProducer))

    done
  }

}

object InstantMessageForwarder {
  def apply()(implicit as: ActorSystem, mat: Materializer, ec: ExecutionContext): InstantMessageForwarder = new InstantMessageForwarder()
}