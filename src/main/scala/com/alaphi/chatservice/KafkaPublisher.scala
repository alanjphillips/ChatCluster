package com.alaphi.chatservice

import akka.Done
import akka.actor.ActorSystem
import akka.kafka.{ProducerMessage, ProducerSettings}
import akka.kafka.scaladsl.Producer
import akka.stream._
import akka.stream.scaladsl.{Sink, Source}
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.{ByteArraySerializer, StringSerializer}
import io.circe.syntax._

import scala.concurrent.{ExecutionContext, Future}
import com.alaphi.chatservice.Message._

class KafkaPublisher(topicOut: String, numPartitions: Int = 3)(implicit system: ActorSystem, ec: ExecutionContext) {

  val decider: Supervision.Decider = {
    case _  => Supervision.Resume
  }

  implicit val materializer = ActorMaterializer(
    ActorMaterializerSettings(system).withSupervisionStrategy(decider))

  val producerSettings = ProducerSettings(system, new ByteArraySerializer, new StringSerializer)
    .withBootstrapServers("kafka-1:9092,kafka-2:9093,kafka-3:9094")

  val kafkaProducer = producerSettings.createKafkaProducer()

  val bufferSize: Int = 1000

  val queue =
    Source.queue[Event](bufferSize, OverflowStrategy.backpressure)
      .map { msg =>
        val partition = math.abs(msg.conversationKey.hashCode) % numPartitions
        val json = msg.asJson.noSpaces
        ProducerMessage.Message(new ProducerRecord[Array[Byte], String](topicOut, partition, null, json), msg)
      }
      .via(Producer.flow(producerSettings))
      .to(Sink.ignore)
      .run

  def send(event: Event): Future[QueueOfferResult] = {
    queue.offer(event)
  }
}

object KafkaPublisher {
  def apply(topicOut: String, numPartitions: Int)
           (implicit system: ActorSystem, ec: ExecutionContext)
  : KafkaPublisher = new KafkaPublisher(topicOut, numPartitions)
}