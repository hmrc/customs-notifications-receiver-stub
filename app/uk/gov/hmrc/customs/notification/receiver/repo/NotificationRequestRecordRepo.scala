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

import org.mongodb.scala.model.{FindOneAndReplaceOptions, IndexModel, IndexOptions}
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

  //TODO make builder function to convert NotificationRequest -> NotificationRequestRecord
  def upsertNotificationRequestRecordByCsId(notificationRequestRecord: NotificationRequestRecord): Future[Unit] = {
    val filter: Bson = equal("notification.csid", notificationRequestRecord.notification.csid)

    collection.findOneAndReplace(
      filter = filter,
      replacement = notificationRequestRecord,
      options = FindOneAndReplaceOptions().upsert(true)).toFuture().map(_ => ())
  }

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

  private def countNotificationsByFilter(filter: conversions.Bson): Future[Int] = {
    collection.countDocuments(filter).toFuture().map(_.toInt)
  }

  //  private def sortNotificationRequestRecordsByDateAscending(notificationRequestRecords: Seq[NotificationRequestRecord]): Seq[NotificationRequestRecord] = {
  //    notificationRequestRecords.sortWith((thisRecord, nextRecord) =>
  //      thisRecord.timeReceived.getOrElse(throw new RuntimeException("Error(sortNotificationRequestRecordsByDateAscending): timeReceived(1) is missing"))
  //        .isBefore(nextRecord.timeReceived.getOrElse(throw new RuntimeException("Error(sortNotificationRequestRecordsByDateAscending): timeReceived(2) is missing")))
  //    )
  //  }
}
