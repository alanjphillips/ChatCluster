package com.alaphi.chatservice

import akka.actor.{Actor, ActorLogging, PoisonPill, Props, ReceiveTimeout}
import akka.cluster.sharding.ShardRegion.Passivate
import akka.persistence.PersistentActor
import com.alaphi.chatservice.Message.MessageData

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._

class ConversationActor(imForwarder: InstantMessageForwarder) extends PersistentActor with ActorLogging {

  var latestChatter: ListBuffer[MessageData] = ListBuffer()
  var conversationMsgSeq = 0

  val latestChatterLimit = 1000

  // passivate the entity when no activity
  context.setReceiveTimeout(2.minutes)

  override def persistenceId: String = self.path.parent.name + "-" + self.path.name

  override def receiveCommand: Receive = command orElse request orElse passivate orElse Actor.ignoringBehavior

  override def receiveRecover: Receive = recover orElse Actor.ignoringBehavior

  def updateState(event: MessageEvent): Unit = {
    if (latestChatter.size >= latestChatterLimit)
      latestChatter.remove(0)

    latestChatter.append(new MessageData(event.sender, event.body, conversationMsgSeq))
    conversationMsgSeq += 1
  }

  val command: Receive = {
    case msg: TextMessageCommand =>
      persistAll(List(MessageEvent(msg.conversationKey, msg.sender, msg.recipients, conversationMsgSeq, msg.body))) { mEvt =>
        updateState(mEvt)
        imForwarder.deliverMessage(mEvt)
      }
  }

  val recover: Receive = {
    case msgEvt: MessageEvent => updateState(msgEvt)
  }

  val request: Receive = {
    case GetLatestChatter(conversationKey, numMsgs) =>
      imForwarder.deliverLatestChat(
        LatestChatter(conversationKey, latestChatter.toList.takeRight(numMsgs))
      )
  }

  val passivate: Receive = {
    case ReceiveTimeout => context.parent ! Passivate(PoisonPill)
  }

}

object ConversationActor {
  def props(imForwarder: InstantMessageForwarder): Props = Props(new ConversationActor(imForwarder))
}
