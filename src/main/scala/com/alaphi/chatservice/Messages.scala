package com.alaphi.chatservice

import io.circe._
import io.circe.generic.semiauto._

object Message {
  type SenderMsg = (String, String) // Sender and Message Pair

  implicit val messageEventEncoder: Encoder[MessageEvent] = deriveEncoder[MessageEvent]
  implicit val latestChatterEncoder: Encoder[LatestChatter] = deriveEncoder[LatestChatter]

  implicit val textMessageDecoder: Decoder[TextMessageCommand] = deriveDecoder[TextMessageCommand]
}

import Message._

case class TextMessageCommand(conversationKey: String, sender: String, recipients: List[String], body: String)
case class MessageEvent(conversationKey: String, sender: String, recipients: List[String], conversationMsgSeq: Int, body: String)
case class GetLatestChatter(conversationKey: String, numMsgs: Int)
case class LatestChatter(conversationKey: String, latestMsgSeq: Int, latestChatter: List[SenderMsg])


