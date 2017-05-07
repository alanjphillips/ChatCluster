package com.alaphi.chatservice

import akka.actor.{Actor, ActorLogging, Props}
import akka.persistence.PersistentActor

import scala.collection.mutable.ListBuffer
import scala.concurrent.duration._

case class TextMessage(conversationKey: String, body: String)
case class MessageEvent(conversationKey: String, conversationMsgSeq: Int, body: String)
case class GetLatestChatter(conversationKey: String, numMsgs: Int)
case class LatestChatter(conversationKey: String, latestMsgSeq: Int, latestChatter: List[String])

class ConversationActor(imForwarder: InstantMessageForwarder) extends PersistentActor with ActorLogging {

  var latestChatter: ListBuffer[String] = ListBuffer()
  var conversationMsgSeq = 0

  val latestChatterLimit = 1000

  // passivate the entity when no activity
  context.setReceiveTimeout(2.minutes)

  override def persistenceId: String = self.path.parent.name + "-" + self.path.name

  override def receive = receiveCommand orElse receiveRecover orElse receiveRequest orElse Actor.emptyBehavior

  def updateState(event: MessageEvent): Unit = {
    if (latestChatter.size >= latestChatterLimit) latestChatter.remove(0)

    latestChatter ++ event.body
    conversationMsgSeq += 1
  }

  val receiveCommand: Receive = {
    case msg: TextMessage =>
      persistAll(List(MessageEvent(msg.conversationKey, conversationMsgSeq, msg.body))) { mEvt =>
        updateState(mEvt)
        imForwarder.deliverMessage(mEvt)
      }
  }

  val receiveRecover: Receive = {
    case msgEvt: MessageEvent => updateState(msgEvt)
  }

  val receiveRequest: Receive = {
    case GetLatestChatter(conversationKey, numMsgs) =>
      sender ! LatestChatter(conversationKey, conversationMsgSeq, latestChatter.toList.takeRight(numMsgs))
  }

}

object ConversationActor {
  def props(imForwarder: InstantMessageForwarder): Props = Props(new ConversationActor(imForwarder))
}
