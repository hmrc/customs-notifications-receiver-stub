/*
 * Copyright 2022 HM Revenue & Customs
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

package uk.gov.hmrc.customs.notification.receiver.models

import java.util.UUID

import play.api.libs.json._

case class Header(name: String, value: String)

object Header{
  implicit val formats: Format[Header] = Json.format[Header]
}

case class ConversationId(id: UUID) extends AnyVal {
  override def toString: String = id.toString
}
object ConversationId {
  implicit val conversationIdJF = new Format[ConversationId] {
    def writes(conversationId: ConversationId): JsValue = JsString(conversationId.id.toString)
    def reads(json: JsValue): JsResult[ConversationId] = json match {
      case JsNull => JsError()
      case _ => JsSuccess(ConversationId(json.as[UUID]))
    }
  }
}

case class CsId(id: UUID) extends AnyVal {
  override def toString: String = id.toString
}
object CsId {
  implicit val clientSubscriptionIdJF = new Format[CsId] {
    def writes(csid: CsId): JsString = JsString(csid.id.toString)
    def reads(json: JsValue): JsResult[CsId] = json match {
      case JsNull => JsError()
      case _ => JsSuccess(CsId(json.as[UUID]))
    }
  }
}

case class NotificationRequest(
  csid: CsId,
  conversationId: ConversationId,
  authHeaderToken: String,
  outboundCallHeaders: Seq[Header],
  xmlPayload: String
)

object NotificationRequest {
  private implicit val headerFormats: Format[Header] = Json.format[Header]
  implicit val formats: Format[NotificationRequest] = Json.format[NotificationRequest]
}

