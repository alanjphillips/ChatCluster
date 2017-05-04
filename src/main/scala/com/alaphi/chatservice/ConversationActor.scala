package com.alaphi.chatservice

import akka.actor.{Actor, ActorLogging, Props}

case class TextMessage(conversationKey: String, textMessage: String)
case class Event(conversationKey: String, textMessage: String)

class ConversationActor extends Actor with ActorLogging {

  override def receive: Receive = ???

}

object ConversationActor {

  def props(): Props = Props(new ConversationActor)

}
