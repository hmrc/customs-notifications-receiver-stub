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

package integration.repo

import java.util.UUID
import javax.inject.Singleton

import uk.gov.hmrc.customs.notification.receiver.models._
import uk.gov.hmrc.customs.notification.receiver.repo.NotificationRepo

import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

@Singleton
class InMemoryPersistenceService extends NotificationRepo {

  private val notificationsByCsidMap = scala.collection.mutable.Map[UUID, Seq[NotificationRequest]]()

  def persist(notificationRequest: NotificationRequest): Future[Boolean] = {
    notificationsByCsidMap.get(notificationRequest.csid).fold[Unit](notificationsByCsidMap.put(notificationRequest.csid, Seq(notificationRequest))) { notifications: Seq[NotificationRequest] =>
      val newList = notifications :+ notificationRequest
      notificationsByCsidMap.put(notificationRequest.csid, newList)
    }
    Future.successful(true)
  }

  def notificationsByCsId(csid: CsId): Future[Seq[NotificationRequest]] = {
    Future.successful(
      notificationsByCsidMap.get(csid).fold[Seq[NotificationRequest]](Seq.empty)(ns => ns)
    )
  }

  def clearAll(): Future[Unit] = Future.successful(notificationsByCsidMap.clear)

  def notificationCountByCsId(csid: CsId): Future[Int] = notificationsByCsId(csid).map(ns => ns.size)
}
