package com.alaphi.chatservice

import akka.actor.{Actor, ActorLogging, Props}
import akka.persistence.PersistentActor

import scala.collection.mutable.{ListBuffer, MutableList}
import scala.concurrent.duration._

case class TextMessage(conversationKey: String, body: String)
case class MessageEvent(conversationKey: String, body: String)

class ConversationActor extends PersistentActor with ActorLogging {

  var latestChatter: ListBuffer[String] = ListBuffer()
  val latestChatterLimit = 1000

  override def persistenceId: String = self.path.parent.name + "-" + self.path.name

  // passivate the entity when no activity
  context.setReceiveTimeout(2.minutes)

  def updateState(event: MessageEvent): Unit = {
    if (latestChatter.size >= latestChatterLimit)
      latestChatter.remove(0)

    latestChatter ++ event.body
  }

  val receiveCommand: Receive = {
    case msg: TextMessage =>
      persistAll(List(MessageEvent(msg.conversationKey,msg.body))) {
        updateState
        // Send output MessageEvent over Kafka here which is to be delivered to conversation members
      }
  }

  val receiveRecover: Receive = {
    case msgEvt: MessageEvent => updateState(msgEvt)
  }

}

object ConversationActor {

  def props(): Props = Props(new ConversationActor)

}
