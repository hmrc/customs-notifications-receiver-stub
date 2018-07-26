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

import play.api.libs.json.{Format, Json}

case class CountsByConversationId(conversationId: ConversationId, count: Int)
object CountsByConversationId {
  implicit val format: Format[CountsByConversationId] = Json.format[CountsByConversationId]
}

case class CountsByGroupedByCsidAndConversationId(csid: CsId, countsByConversationId: Seq[CountsByConversationId])
object CountsByGroupedByCsidAndConversationId {
  implicit val format: Format[CountsByGroupedByCsidAndConversationId] = Json.format[CountsByGroupedByCsidAndConversationId]
}