/*
 * Copyright 2018 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package util

import java.util.UUID

import play.api.libs.json.{JsValue, Json}

import scala.util.control.NonFatal
import scala.xml.{Node, NodeSeq, Utility, XML}

object TestData {

  val CsidOne: UUID = UUID.fromString("ffff01f9-ec3b-4ede-b263-61b626dde232")
  val CsidTwo: UUID = UUID.fromString("ffff01f9-ec3b-4ede-b263-61b626dde239")
  val ConversationIdOne: UUID = UUID.fromString("eaca01f9-ec3b-4ede-b263-61b626dde232")
  val ConversationIdTwo: UUID = UUID.fromString("eaca01f9-ec3b-4ede-b263-61b626dde239")
  val HeaderOne: (String, String) = "h1" -> "v1"
  val HeaderTwo: (String, String) = "h2" -> "v2"
  val XmlPayload : NodeSeq = <stuff><moreXml>Stuff</moreXml></stuff>

  def notificationRequestJson(csid: UUID, conversationId: UUID, xmlPayload: NodeSeq = XmlPayload): JsValue = Json.parse(notificationRequest(csid, conversationId))

  def notificationRequest(csid: UUID, conversationId: UUID, xmlPayload: NodeSeq = XmlPayload): String =
    s"""{
      |  "csid": "$csid",
      |  "conversationId": "$conversationId",
      |  "authHeaderToken": "Basic $csid",
      |  "outboundCallHeaders": [
      |    {
      |      "name": "User-Agent",
      |      "value": "Customs Declaration Service"
      |    },
      |    {
      |      "name": "Content-Type",
      |      "value": "application/xml"
      |    },
      |    {
      |      "name": "Authorization",
      |      "value": "Basic $csid"
      |    },
      |    {
      |      "name": "Content-Length",
      |      "value": "39"
      |    },
      |    {
      |      "name": "X-Conversation-ID",
      |      "value": "$conversationId"
      |    }
      |  ],
      |  "xmlPayload": "${xmlPayload.toString}"
      |}""".stripMargin

  def notificationsResultJson(notifications: String *): JsValue = Json.parse {
    s"""[${notifications.mkString(",")}]""".stripMargin
  }

  protected def string2xml(s: String): Node = {
    val xml = try {
      XML.loadString(s)
    } catch {
      case NonFatal(thr) => org.scalatest.Assertions.fail("Not an xml: " + s, thr)
    }
    Utility.trim(xml)
  }
}




