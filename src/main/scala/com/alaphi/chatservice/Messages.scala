package com.alaphi.chatservice

object Message {
  type SenderMsg = (String, String) // Sender and Message Pair
}

import Message._

case class TextMessage(conversationKey: String, sender: String, recipients: List[String], body: String)
case class MessageEvent(conversationKey: String, sender: String, recipients: List[String], conversationMsgSeq: Int, body: String)
case class GetLatestChatter(conversationKey: String, numMsgs: Int)
case class LatestChatter(conversationKey: String, latestMsgSeq: Int, latestChatter: List[SenderMsg])
