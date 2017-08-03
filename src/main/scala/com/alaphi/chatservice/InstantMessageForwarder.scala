package com.alaphi.chatservice

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.ProducerSettings
import akka.kafka.scaladsl.Producer
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Materializer, Supervision}
import akka.stream.scaladsl.Source
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}
import io.circe.syntax._

import scala.concurrent.{ExecutionContext, Future}
import com.alaphi.chatservice.Message._

class InstantMessageForwarder(numPartitions: Int = 3)(implicit system: ActorSystem, ec: ExecutionContext) {

  val decider: Supervision.Decider = {
    case _  => Supervision.Resume
  }

  implicit val materializer = ActorMaterializer(
    ActorMaterializerSettings(system).withSupervisionStrategy(decider))

  val producerSettings = ProducerSettings(system, new ByteArraySerializer, new StringSerializer)
    .withBootstrapServers("kafka-1:9092,kafka-2:9093,kafka-3:9094")

  val kafkaProducer = producerSettings.createKafkaProducer()

  def deliverMessage(message: MessageEvent): Future[Done] = send(message, "instant_message_out")

  def deliverLatestChat(chatMessages: LatestChatter): Future[Done] = send(chatMessages, "latest_messages_block")

  def send(event: Event, dest: String): Future[Done] =
    Source.single(event)
      .map { msg =>
        val partition = math.abs(msg.conversationKey.hashCode) % numPartitions
        val json = msg.asJson.noSpaces
        new ProducerRecord[Array[Byte], String](dest, partition, null, json) // Refactor: use key to determine partition
      }
      .runWith(Producer.plainSink(producerSettings, kafkaProducer))

}

object InstantMessageForwarder {
  def apply(numPartitions: Int)
           (implicit system: ActorSystem, ec: ExecutionContext)
  : InstantMessageForwarder = new InstantMessageForwarder(numPartitions)
}