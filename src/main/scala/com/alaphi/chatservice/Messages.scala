package com.alaphi.chatservice

import io.circe._
import cats.syntax.functor._
import io.circe.generic.semiauto._

object Message {
  type MessageData = (String, String, Int) // Sender, Message Pair, MsgSeq

  implicit val messageEventEncoder: Encoder[MessageEvent] = deriveEncoder[MessageEvent]
  implicit val latestChatterEncoder: Encoder[LatestChatter] = deriveEncoder[LatestChatter]

  implicit val textMessageDecoder: Decoder[TextMessageCommand] = deriveDecoder[TextMessageCommand]
  implicit val getLatestChatterDecoder: Decoder[GetLatestChatter] = deriveDecoder[GetLatestChatter]

  implicit val decodeCommand: Decoder[Command] =
    List[Decoder[Command]](
      Decoder[TextMessageCommand].widen,
      Decoder[GetLatestChatter].widen
    ).reduceLeft(_ or _)
}

import Message._

trait Command

case class TextMessageCommand(conversationKey: String, sender: String, recipients: List[String], body: String) extends Command
case class GetLatestChatter(conversationKey: String, numMsgs: Int) extends Command

case class MessageEvent(conversationKey: String, sender: String, recipients: List[String], conversationMsgSeq: Int, body: String)
case class LatestChatter(conversationKey: String, latestChatter: List[MessageData])


