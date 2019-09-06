/*
 * Copyright 2019 HM Revenue & Customs
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


import javax.inject.{Inject, Singleton}
import play.api.libs.json._
import reactivemongo.api.Cursor
import reactivemongo.api.indexes.{Index, IndexType}
import reactivemongo.bson.BSONObjectID
import reactivemongo.play.json.JsObjectDocumentWriter
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.notification.receiver.logging.LoggingHelper._
import uk.gov.hmrc.customs.notification.receiver.models.{ConversationId, CsId, NotificationRequest, NotificationRequestRecord}
import uk.gov.hmrc.mongo.ReactiveRepository

import scala.collection.immutable.Seq
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MongoNotificationsRepo @Inject()(mongoDbProvider: MongoDbProvider,
                                       errorHandler: NotificationRepositoryErrorHandler,
                                       logger: CdsLogger)
                                      (implicit ec: ExecutionContext)
  extends ReactiveRepository[NotificationRequestRecord, BSONObjectID](
    collectionName = "notifications",
    mongo = mongoDbProvider.mongo,
    domainFormat = NotificationRequestRecord.format
  ) with NotificationRepo {

  private implicit val format: Format[NotificationRequestRecord] = NotificationRequestRecord.format

  override def indexes: Seq[Index] = Seq(
    Index(
      key = Seq("notification.csid" -> IndexType.Ascending, "timeReceived" -> IndexType.Descending),
      name = Some("csid-timeReceived-Index"),
      unique = false
    ),
    Index(
      key = Seq("notification.conversationId" -> IndexType.Ascending, "timeReceived" -> IndexType.Descending),
      name = Some("conversationId-timeReceived-Index"),
      unique = false
    )
  )

  def persist(n: NotificationRequest): Future[Boolean] = {
    logger.debug(s"${logMsgPrefix(n)} saving clientNotification: $n")

    lazy val errorMsg = s"Client Notification not saved for clientSubscriptionId ${n.csid}"
    val record = NotificationRequestRecord(n)
    val selector = Json.obj("_id" -> record.id)
    val update = Json.obj("$currentDate" -> Json.obj("timeReceived" -> true), "$set" -> record)
    collection.update(ordered = false).one(selector, update, upsert = true).map {
      writeResult => errorHandler.handleSaveError(writeResult, errorMsg, record)
    }

  }

  def notificationsByCsId(csid: CsId): Future[Seq[NotificationRequest]] =
  {
    logger.debug(s"fetching clientNotification(s) with csid: ${csid.toString}")
    val selector = Json.obj("notification.csid" -> csid)
    val sortOrder = Json.obj("timeReceived" -> 1)
    val cursor = collection.find(selector, None).sort(sortOrder).cursor().collect[Seq](Int.MaxValue, Cursor.FailOnError[Seq[NotificationRequestRecord]]())
    cursor.map(ns => ns.map(record => record.notification))
  }

  def notificationsByConversationId(conversationId: ConversationId): Future[Seq[NotificationRequest]] =
  {
    logger.debug(s"fetching clientNotification(s) with conversationId: ${conversationId.toString}")
    val selector = Json.obj("notification.conversationId" -> conversationId)
    val sortOrder = Json.obj("timeReceived" -> 1)
    val cursor = collection.find(selector, None).sort(sortOrder).cursor().collect[Seq](Int.MaxValue, Cursor.FailOnError[Seq[NotificationRequestRecord]]())
    cursor.map(ns => ns.map(record => record.notification))
  }

  def notificationCountByCsId(csid: CsId): Future[Int] =
  {
    logger.debug(s"counting clientNotification(s) with csid: ${csid.toString}")
    val selector = Json.obj("notification.csid" -> csid)
    count(selector)
  }

  def notificationCountByConversationId(conversationId: ConversationId): Future[Int] =
  {
    logger.debug(s"counting clientNotification(s) with conversationId: ${conversationId.toString}")
    val selector = Json.obj("notification.conversationId" -> conversationId)
    count(selector)
  }

  def notificationCount: Future[Int] =
  {
    logger.debug("counting all clientNotifications")
    count(Json.obj())
  }

  def clearAll(): Future[Unit] = {
    collection.drop(failIfNotFound = true).map{isOk =>
      logger.debug(s"clear all result=$isOk")
      ()
    }
  }
}
