package com.alaphi.chatservice

import akka.actor.{Actor, ActorLogging, Props}

case class TextMessage(conversationKey: String, body: String)
case class Event(conversationKey: String, body: String)

class ConversationActor extends Actor with ActorLogging {

  override def receive: Receive = ???

}

object ConversationActor {

  def props(): Props = Props(new ConversationActor)

}
