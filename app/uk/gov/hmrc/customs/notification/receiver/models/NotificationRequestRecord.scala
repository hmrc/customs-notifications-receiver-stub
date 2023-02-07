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

import org.joda.time.DateTime
import org.bson.types.ObjectId
import play.api.libs.json.{Format, JsPath, Json, OFormat, Reads, Writes}
import uk.gov.hmrc.mongo.play.json.formats.MongoJodaFormats
import uk.gov.hmrc.mongo.play.json.formats.MongoFormats
import play.api.libs.functional.syntax._
import play.api.libs.json.Format.GenericFormat
import play.api.libs.json.OFormat.oFormatFromReadsAndOWrites
import uk.gov.hmrc.auth.core.retrieve.LoginTimes.dateTimeReads

case class NotificationRequestRecord(
                                      notification: NotificationRequest,
                                      timeReceived: Option[DateTime] = Some(DateTime.now()),
                                      id: ObjectId = ObjectId.get()
                                    )
case object NotificationRequestRecord {
  implicit val dateFormats = MongoJodaFormats.dateTimeFormat
  //implicit val format = Json.format[NotificationRequestRecord]
  implicit val objectIdFormat: Format[ObjectId] = MongoFormats.objectIdFormat
  implicit val format: Format[NotificationRequestRecord] = Format(reads, writes)
  implicit val reads: Reads[NotificationRequestRecord] = (
    (JsPath \ "notification").read[NotificationRequest] and
    (JsPath \ "timeReceived").readNullable[DateTime] and
    (JsPath \ "id").read[ObjectId]
    )(NotificationRequestRecord.apply _)
  implicit val writes: Writes[NotificationRequestRecord] = (
    (JsPath \ "notification").write[NotificationRequest] and
    (JsPath \ "timeReceived").writeNullable[DateTime] and
    (JsPath \ "id").write[ObjectId]
    )(unlift(NotificationRequestRecord.unapply)
  )
}