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

package integration.repo

import javax.inject.Singleton
import play.api.test.Helpers
import uk.gov.hmrc.customs.notification.receiver.models._
import scala.concurrent.Future

@Singleton
class InMemoryPersistenceService {

  private implicit val ec = Helpers.stubControllerComponents().executionContext

  private val notifications = new scala.collection.mutable.ListBuffer[NotificationRequest]

  def persist(notificationRequest: NotificationRequest): Future[Boolean] = {
    notifications += notificationRequest
    Future.successful(true)
  }

  def notificationsByCsId(csid: CsId): Future[Seq[NotificationRequest]] = {
    Future.successful {
      notifications.filter(n => n.csid == csid)
    }
  }

  def notificationsByConversationId(conversationId: ConversationId): Future[Seq[NotificationRequest]] = {
    Future.successful {
      notifications.filter(n => n.conversationId == conversationId)
    }
  }

  def clearAll(): Future[Unit] = Future.successful{
    notifications.clear()
  }

  def notificationCountByCsId(csid: CsId): Future[Int] = notificationsByCsId(csid).map(ns => ns.size)(ec)

  def notificationCountByConversationId(conversationId: ConversationId): Future[Int] = notificationsByConversationId(conversationId).map(ns => ns.size)(ec)

  def notificationCount: Future[Int] = Future.successful(notifications.size)
}
