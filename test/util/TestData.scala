/*
 * Copyright 2023 HM Revenue & Customs
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

import org.bson.types.ObjectId
import org.joda.time.DateTime

import java.util.UUID
import play.api.libs.json.{JsValue, Json}
import uk.gov.hmrc.customs.notification.receiver.models.{ConversationId, CsId, Header, NotificationRequest, NotificationRequestRecord}

import scala.util.control.NonFatal
import scala.xml._

object TestData {
  val csId1: CsId = CsId(UUID.fromString("ffff01f9-ec3b-4ede-b263-61b626dde232"))
  val csId2: CsId = CsId(UUID.fromString("ffff01f9-ec3b-4ede-b263-61b626dde239"))
  val csId3: CsId = CsId(UUID.fromString("ffff01f9-ec3b-4ede-b263-61b626dde234"))
  val conversationId1: ConversationId = ConversationId(UUID.fromString("eaca01f9-ec3b-4ede-b263-61b626dde232"))
  val conversationId2: ConversationId = ConversationId(UUID.fromString("eaca01f9-ec3b-4ede-b263-61b626dde239"))
  val conversationId3: ConversationId = ConversationId(UUID.fromString("eaca01f9-ec3b-4ede-b263-61b626dde231"))
  val header1: Header = Header(name = "testHeader1", value = "value1")
  val header2: Header = Header(name = "testHeader2", value = "value2")
  val header3: Header = Header(name = "testHeader3", value = "value3")
  val header4: Header = Header(name = "testHeader4", value = "value4")
  val header5: Header = Header(name = "testHeader5", value = "value5")
  val header6: Header = Header(name = "testHeader6", value = "value6")
  //TODO revert to seq?
  val headers1: List[Header] = List(header1, header2)
  val headers2: List[Header] = List(header3, header4)
  val headers3: List[Header] = List(header5, header6)
  lazy val objectId1: ObjectId = new ObjectId
  lazy val objectId2: ObjectId = new ObjectId
  lazy val objectId3: ObjectId = new ObjectId
  lazy val authHeaderToken: String = "testAuthHeaderToken"
  lazy val xmlPayload: String = "testXmlPayload"
  lazy val notificationRequest1: NotificationRequest = NotificationRequest(
    csid = csId1,
    conversationId = conversationId1,
    authHeaderToken = authHeaderToken,
    outboundCallHeaders = headers1,
    xmlPayload = xmlPayload)
  lazy val notificationRequest2: NotificationRequest = NotificationRequest(
    csid = csId2,
    conversationId = conversationId2,
    authHeaderToken = authHeaderToken,
    outboundCallHeaders = headers2,
    xmlPayload = xmlPayload)
  lazy val notificationRequest3: NotificationRequest = NotificationRequest(
    csid = csId3,
    conversationId = conversationId3,
    authHeaderToken = authHeaderToken,
    outboundCallHeaders = headers3,
    xmlPayload = xmlPayload)
  val notificationRequestRecord1: NotificationRequestRecord = NotificationRequestRecord(
    notification = notificationRequest1,
    //TODO make the time set
    timeReceived = DateTime.now().toDateTimeISO,
    _id = objectId1)
  val notificationRequestRecord2: NotificationRequestRecord = NotificationRequestRecord(
    notification = notificationRequest2,
    //TODO make the time set
    timeReceived = DateTime.now().toDateTimeISO,
    _id = objectId2)
  val notificationRequestRecord3: NotificationRequestRecord = NotificationRequestRecord(
    notification = notificationRequest3,
    //TODO make the time set
    timeReceived = DateTime.now().toDateTimeISO,
    _id = objectId3)

  //TODO delete these?
  val HeaderOne: (String, String) = "h1" -> "v1"
  val HeaderTwo: (String, String) = "h2" -> "v2"
  val XmlPayload : NodeSeq = <stuff><moreXml>Stuff</moreXml></stuff>
  val AuthToken = "AUTH_TOKEN"

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
