package com.alaphi.chatservice

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.{Backoff, BackoffSupervisor}
import akka.stream.Materializer
import scala.concurrent.duration._

class InstantMessageConsumer(chatRegion: ActorRef)(implicit mat: Materializer, system: ActorSystem) {

  def start = {
    val supervisor = BackoffSupervisor.props(
      Backoff.onFailure(
        MessageConsumerActor.props(chatRegion),
        childName = "MessageConsumerActor",
        minBackoff = 3.seconds,
        maxBackoff = 30.seconds,
        randomFactor = 0.2
      )
    )
    system.actorOf(supervisor, name = "messageConsumerActorSupervisor")
  }

}

object InstantMessageConsumer {
  def apply(chatRegion: ActorRef)(implicit mat: Materializer, system: ActorSystem): InstantMessageConsumer = new InstantMessageConsumer(chatRegion)
}
