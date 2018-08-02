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

package uk.gov.hmrc.customs.notification.receiver.models

import org.joda.time.DateTime
import play.api.libs.json.Json
import reactivemongo.bson.BSONObjectID
import uk.gov.hmrc.mongo.json.ReactiveMongoFormats

case class NotificationRequestRecord(
                notification: NotificationRequest,
                timeReceived: Option[DateTime] = None,
                id: BSONObjectID = BSONObjectID.generate
              )
case object NotificationRequestRecord {
  implicit val dateFormats = ReactiveMongoFormats.dateTimeFormats
  implicit val idFormat = reactivemongo.play.json.BSONFormats.BSONObjectIDFormat
  implicit val format = ReactiveMongoFormats.mongoEntity(Json.format[NotificationRequestRecord])
}
