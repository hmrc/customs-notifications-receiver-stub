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

package uk.gov.hmrc.customs.notification.receiver.repo

import com.google.inject.ImplementedBy
import uk.gov.hmrc.customs.notification.receiver.models.{ConversationId, CsId, NotificationRequest}

import scala.concurrent.Future

@ImplementedBy(classOf[MongoNotificationsRepo])
trait NotificationRepo {

  def persist(notificationRequest: NotificationRequest): Future[Boolean]

  def notificationsByCsId(csid: CsId): Future[Seq[NotificationRequest]]

  def notificationsByConversationId(conversationId: ConversationId): Future[Seq[NotificationRequest]]

  def notificationCountByCsId(csid: CsId): Future[Int]

  def notificationCountByConversationId(conversationId: ConversationId): Future[Int]

  def notificationCount: Future[Int]

  def clearAll(): Future[Unit]
}
