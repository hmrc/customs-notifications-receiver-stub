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

package uk.gov.hmrc.customs.notification.receiver.repo

import org.bson.types.ObjectId
import org.joda.time.DateTime
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import org.mongodb.scala.model.Indexes.compoundIndex
import uk.gov.hmrc.customs.notification.receiver.models.{ConversationId, CsId, NotificationRequest, NotificationRequestRecord}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}
import org.mongodb.scala.model.Indexes.{ascending, descending}
import org.mongodb.scala.bson.conversions
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.equal

import scala.concurrent.{ExecutionContext, Future}
import javax.inject.{Inject, Singleton}

@Singleton
class NotificationRequestRecordRepo @Inject()(mongoComponent: MongoComponent)(implicit ec: ExecutionContext)
  extends PlayMongoRepository[NotificationRequestRecord](
    mongoComponent = mongoComponent,
    collectionName = "notifications",
    domainFormat = NotificationRequestRecord.format,
    extraCodecs = Seq(
      Codecs.playFormatCodec(NotificationRequest.format),
      Codecs.playFormatCodec(CsId.format),
      Codecs.playFormatCodec(ConversationId.format)
    ),
    indexes = Seq(
      IndexModel(
        compoundIndex(ascending("notification.csid"), descending("timeReceived")),
        IndexOptions()
          .name("csid-timeReceived-Index")
          .unique(false)
      ),
      IndexModel(
        compoundIndex(ascending("notification.conversationId"), descending("timeReceived")),
        IndexOptions()
          .name("conversationId-timeReceived-Index")
          .unique(false)))
  ) {

  def buildNotificationRequestRecord(notificationRequest: NotificationRequest): NotificationRequestRecord = {
    NotificationRequestRecord(notification = notificationRequest, timeReceived = DateTime.now().toDateTimeISO, _id = new ObjectId())
  }

  //TODO make builder function to convert NotificationRequest -> NotificationRequestRecord
  def insertNotificationRequestRecord(notificationRequestRecord: NotificationRequestRecord): Future[Unit] = {
    collection.insertOne(notificationRequestRecord).toFuture().map(_ => ())
  }

  def findAllByCsId(csId: CsId): Future[Seq[NotificationRequest]] = {
    val filter: Bson = buildCsIdFilter(csId)
    findAllWithFilterAndSort(filter)
  }

  def findAllByConversationId(conversationId: ConversationId): Future[Seq[NotificationRequest]] = {
    val filter: Bson = buildConversationIdFilter(conversationId)
    findAllWithFilterAndSort(filter)
  }

  def countNotificationsByCsId(csId: CsId): Future[Int] = {
    countNotificationsByFilter(buildCsIdFilter(csId))
  }

  def countNotificationsByConversationId(conversationId: ConversationId): Future[Int] = {
    countNotificationsByFilter(buildConversationIdFilter(conversationId))
  }

  def countAllNotifications(): Future[Int] = {
    collection.countDocuments().toFuture().map(_.toInt)
  }

  def dropDb(): Future[Unit] = {
    collection.drop().toFuture().map(_ => ())
  }

  private def buildCsIdFilter(csId: CsId): conversions.Bson = {
    equal("notification.csid", csId)
  }

  private def buildConversationIdFilter(conversationId: ConversationId): conversions.Bson = {
    equal("notification.conversationId", conversationId)
  }

  private def findAllWithFilterAndSort(filter: Bson): Future[Seq[NotificationRequest]] = {
    for {
      notificationRequestRecords <- collection.find(filter).toFuture()
    } yield {
      val sortedNotificationRequestRecords = sortNotificationRequestRecordsByDateAscending(notificationRequestRecords)
      sortedNotificationRequestRecords.map(_.notification)
    }
  }

  private def countNotificationsByFilter(filter: conversions.Bson): Future[Int] = {
    collection.countDocuments(filter).toFuture().map(_.toInt)
  }

  private def sortNotificationRequestRecordsByDateAscending(notificationRequestRecords: Seq[NotificationRequestRecord]): Seq[NotificationRequestRecord] = {
    notificationRequestRecords.sortWith((thisRecord, nextRecord) => thisRecord.timeReceived.isBefore(nextRecord.timeReceived))
  }

  //These three below are not used outside of testing but useful for that
  def findByCsId(csId: CsId): Future[NotificationRequestRecord] = {
    val filter: Bson = buildCsIdFilter(csId)
    collection.find(filter).toFuture().map(_.toList.head)
  }

  def findByConversationId(conversationId: ConversationId): Future[NotificationRequestRecord] = {
    val filter: Bson = buildConversationIdFilter(conversationId)
    collection.find(filter).toFuture().map(_.toList.head)
  }

  def findAny: Future[NotificationRequestRecord] = {
    collection.find().toFuture().map(_.toList.head)
  }

  def findAnyAsNotificationRequest: Future[Seq[NotificationRequest]] = {
    for {
      notificationRequestRecords <- collection.find().toFuture()
    } yield {
      val sortedNotificationRequestRecords = sortNotificationRequestRecordsByDateAscending(notificationRequestRecords)
      sortedNotificationRequestRecords.map(_.notification)
    }
  }
}
