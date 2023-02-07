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

import org.bson.types.ObjectId

import java.util.UUID
import play.api.libs.json._
import play.api.libs.functional.syntax._
import uk.gov.hmrc.mongo.play.json.formats.MongoFormats

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
  //implicit val formats: Format[NotificationRequest] = Json.format[NotificationRequest]
  implicit val format: Format[NotificationRequest] = Format(reads, writes)
  implicit val reads: Reads[NotificationRequest] = (
    (JsPath \ "csid").read[CsId] and
    (JsPath \ "conversationId").read[ConversationId] and
    (JsPath \ "authHeaderToken").read[String] and
    (JsPath \ "outboundCallHeaders").read[Seq[Header]] and
    (JsPath \ "xmlPayload").read[String]
    )(NotificationRequest.apply _)
  implicit val writes: Writes[NotificationRequest] = (
    (JsPath \ "csid").write[CsId] and
    (JsPath \ "conversationId").write[ConversationId] and
    (JsPath \ "authHeaderToken").write[String] and
    (JsPath \ "outboundCallHeaders").write[Seq[Header]] and
    (JsPath \ "xmlPayload").write[String]
    )(unlift(NotificationRequest.unapply))
}

case class TestX(
                  child: TestChild,
                  timeReceived: String,
                  _id: ObjectId
                )
object TestX{
  //implicit val format: Format[TestX] = Json.format[TestX]
  implicit val objectIdFormat: Format[ObjectId] = MongoFormats.objectIdFormat
//  implicit val objectIdWrites: Writes[ObjectId] = MongoFormats.objectIdWrites
//  implicit val objectIdReads: Reads[ObjectId] = MongoFormats.objectIdReads
  //implicit val format: Format[TestX] = Json.format[TestX]
  implicit val format: Format[TestX] = (
      (__ \ "child").format[TestChild] and
      (__ \ "timeReceived").format[String] and
      (__ \ "_id").format[ObjectId]
  )(TestX.apply, unlift(TestX.unapply))


//  implicit val format: Format[TestX] = Format(reads, writes)
//  implicit val reads: Reads[TestX] = (
//      (JsPath \ "id1").read[String] and
//      (JsPath \ "value").read[String]
//    ) (TestX.apply _)
//
//
//  implicit val writes: Writes[TestX] = (
//      (JsPath \ "id1").write[String] and
//      (JsPath \ "value").write[String]
//    ) (unlift(TestX.unapply))
}

case class TestChild(csid: String, conversationId: String, childValue: String)

object TestChild{
  //implicit val format: Format[TestChild] = Json.format[TestChild]
  implicit val format: Format[TestChild] = (
    (__ \ "csid").format[String] and
    (__ \ "conversationId").format[String] and
    (__ \ "childValue").format[String]
    )(TestChild.apply, unlift(TestChild.unapply))
}

