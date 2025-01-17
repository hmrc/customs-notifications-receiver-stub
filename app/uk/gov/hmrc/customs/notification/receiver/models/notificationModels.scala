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

package uk.gov.hmrc.customs.notification.receiver.models

import uk.gov.hmrc.customs.notification.receiver.Utils
import org.bson.types.ObjectId
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.gov.hmrc.customs.notification.receiver.Utils.hashNotificationContents
import uk.gov.hmrc.mongo.play.json.formats.MongoFormats

import java.time.LocalDateTime
import java.util.UUID

case class Header(name: String, value: String)

object Header{
  implicit val format: Format[Header] = Json.format[Header]
}

case class ConversationId(id: UUID) extends AnyVal {
  override def toString: String = id.toString
}
object ConversationId {
  implicit val format: Format[ConversationId] = new Format[ConversationId] {
    def writes(conversationId: ConversationId): JsValue = JsString(conversationId.id.toString)
    def reads(json: JsValue): JsResult[ConversationId] = json match {
      case JsNull => JsError()
      case _ => JsSuccess(ConversationId(json.as[UUID]))
    }
  }
}

/**
 * Client Subscription ID
 * @param id
 */
case class CsId(id: UUID) extends AnyVal {
  override def toString: String = id.toString
}
object CsId {
  implicit val format: Format[CsId] = new Format[CsId] {
    def writes(csid: CsId): JsString = JsString(csid.id.toString)
    def reads(json: JsValue): JsResult[CsId] = json match {
      case JsNull => JsError()
      case _ => JsSuccess(CsId(json.as[UUID]))
    }
  }
}

case class NotificationRequestRecord(notification: NotificationRequest, _id: ObjectId, hash: String) {
  override def toString: String = {
      s"[_id=${_id}]" +
    s"[notification=${}]"
  }
}

object NotificationRequestRecord {
  def apply(notificationRequest: NotificationRequest): NotificationRequestRecord = {
    new NotificationRequestRecord(notificationRequest, _id = new ObjectId(), hash = hashNotificationContents(notificationRequest.xmlPayload))
  }

  implicit val objectIdFormat: Format[ObjectId] = MongoFormats.objectIdFormat
  implicit val format: Format[NotificationRequestRecord] = (
      (__ \ "notification").format[NotificationRequest] and
        (__ \ "_id").format[ObjectId] and
        (__ \ "hash").format[String]
  )(NotificationRequestRecord.apply, unlift(NotificationRequestRecord.unapply))
}

case class NotificationRequest(csId: CsId,
                               conversationId: ConversationId,
                               authHeaderToken: String,
                               outboundCallHeaders: List[Header],
                               localDateTime: LocalDateTime,
                               xmlPayload: String) {
  override def toString: String = {
    s"[csId=${}]" +
      s"[conversationId=${}]" +
      s"[authHeaderToken=${}]" +
      s"[outboundCallHeaders=${}]" +
      s"[localDateTime=${}]"
  }
}

object NotificationRequest{
  implicit val format: Format[NotificationRequest] = (
    (__ \ "csid").format[CsId] and
    (__ \ "conversationId").format[ConversationId] and
    (__ \ "authHeaderToken").format[String] and
    (__ \ "outboundCallHeaders").format[List[Header]] and
    (__ \ "received").format[LocalDateTime] and //TODO DCWL-2372 we don't need both fields for when notification is received.
    (__ \ "xmlPayload").format[String]
    )(NotificationRequest.apply, unlift(NotificationRequest.unapply))
}
