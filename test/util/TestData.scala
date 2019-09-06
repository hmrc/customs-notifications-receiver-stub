/*
 * Copyright 2019 HM Revenue & Customs
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
import uk.gov.hmrc.customs.notification.receiver.models.{ConversationId, CsId, NotificationRequest}

import scala.util.control.NonFatal
import scala.xml._

object TestData {

  val CsidOne: CsId = CsId(UUID.fromString("ffff01f9-ec3b-4ede-b263-61b626dde232"))
  val CsidTwo: CsId = CsId(UUID.fromString("ffff01f9-ec3b-4ede-b263-61b626dde239"))
  val ConversationIdOne: ConversationId = ConversationId(UUID.fromString("eaca01f9-ec3b-4ede-b263-61b626dde232"))
  val ConversationIdTwo: ConversationId = ConversationId(UUID.fromString("eaca01f9-ec3b-4ede-b263-61b626dde239"))
  val HeaderOne: (String, String) = "h1" -> "v1"
  val HeaderTwo: (String, String) = "h2" -> "v2"
  val XmlPayload : NodeSeq = <stuff><moreXml>Stuff</moreXml></stuff>


  val AuthToken = "AUTH_TOKEN"
  val NotificationRequestOne = NotificationRequest(CsidOne, ConversationIdOne, AuthToken, Seq.empty, s"<foo>OneOfTwo</foo>")
  val NotificationRequestOneTwo = NotificationRequest(CsidOne, ConversationIdOne, AuthToken, Seq.empty, s"<foo>TwoOfTwo</foo>")
  val NotificationRequestTwo = NotificationRequest(CsidTwo, ConversationIdTwo, AuthToken, Seq.empty, s"<foo>$CsidTwo</foo>")

  def notificationRequestJson(csid: CsId, conversationId: ConversationId, xmlPayload: NodeSeq = XmlPayload): JsValue = Json.parse(notificationRequest(csid, conversationId))

  def notificationRequest(csid: CsId, conversationId: ConversationId, xmlPayload: NodeSeq = XmlPayload): String =
    s"""{
      |  "csid": "$csid",
      |  "conversationId": "$conversationId",
      |  "authHeaderToken": "$csid",
      |  "outboundCallHeaders": [
      |    {
      |      "name": "Host",
      |      "value": "localhost"
      |    },
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
      |      "value": "$csid"
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

  val countsJson: JsValue = Json.parse(
    """[
     |  {
     |    "csid": "ffff01f9-ec3b-4ede-b263-61b626dde232",
     |    "countsByConversationId": [
     |      {
     |        "conversationId": "eaca01f9-ec3b-4ede-b263-61b626dde232",
     |        "count": 2
     |      }
     |    ]
     |  },
     |  {
     |    "csid": "ffff01f9-ec3b-4ede-b263-61b626dde239",
     |    "countsByConversationId": [
     |      {
     |        "conversationId": "eaca01f9-ec3b-4ede-b263-61b626dde239",
     |        "count": 1
     |      }
     |    ]
     |  }
     |]""".stripMargin)

  val BadRequestJsonInvalidCsid: JsValue = Json.parse("""{
            |  "code": "BAD_REQUEST",
            |  "message": "Invalid UUID string: 1"
            |}""".stripMargin)

  val BadRequestXmlInvalidXml: NodeSeq = <errorResponse><code>BAD_REQUEST</code><message>Invalid Xml</message></errorResponse>
   val UnsupportedMediaTypeXml: NodeSeq = <errorResponse><code>UNSUPPORTED_MEDIA_TYPE</code><message>The content type header is missing or invalid</message></errorResponse>

  def notificationsResultJson(notifications: String *): JsValue = Json.parse {
    s"""[${notifications.mkString(",")}]""".stripMargin
  }

  def string2xml(s: String): Node = {
    val xml = try {
      XML.loadString(s)
    } catch {
      case NonFatal(thr) => org.scalatest.Assertions.fail("Not an xml: " + s, thr)
    }
    Utility.trim(xml)
  }
}
