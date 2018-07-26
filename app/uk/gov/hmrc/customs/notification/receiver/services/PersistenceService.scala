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

package uk.gov.hmrc.customs.notification.receiver.services

import java.util.UUID
import javax.inject.Singleton

import uk.gov.hmrc.customs.notification.receiver.models._

import scala.collection.immutable.{Iterable, Seq}

@Singleton
class PersistenceService {

  private val notificationsByCsidMap = scala.collection.mutable.Map[UUID, Seq[NotificationRequest]]()

  def persist(notificationRequest: NotificationRequest): Unit = {
    notificationsByCsidMap.get(notificationRequest.csid).fold[Unit](notificationsByCsidMap.put(notificationRequest.csid, Seq(notificationRequest))) { notifications: Seq[NotificationRequest] =>
      val newList = notifications :+ notificationRequest
      notificationsByCsidMap.put(notificationRequest.csid, newList)
    }
  }

  def notificationsById(csid: CsId): Seq[NotificationRequest] = {
    notificationsByCsidMap.get(csid).fold[Seq[NotificationRequest]](Seq.empty)(ns => ns)
  }

  def countsByGroupedByCsidAndConversationId: Seq[CountsGroupedByCsidAndConversationId] = {
    val result = notificationsByCsidMap.map(t => CountsGroupedByCsidAndConversationId(t._1, countsByConversationId(t._2)))
    result.to[collection.immutable.Seq]
  }

  private def countsByConversationId(ns: Seq[NotificationRequest]): Seq[CountsByConversationId] = {
    val groupedBy: Map[ConversationId, Seq[NotificationRequest]] = ns.groupBy(foo => foo.conversationId)
    val listGroupedBy: Map[ConversationId, Int] = groupedBy.mapValues(_.size)
    val result: Iterable[CountsByConversationId] = listGroupedBy.map(t => CountsByConversationId(t._1, t._2))
    result.to[collection.immutable.Seq]
  }

  def clearAll(): Unit = notificationsByCsidMap.clear
}
