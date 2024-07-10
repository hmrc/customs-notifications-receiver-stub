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

import org.mongodb.scala.bson.conversions
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Indexes.{ascending, compoundIndex, descending}
import org.mongodb.scala.model.{IndexModel, IndexOptions}
import org.mongodb.scala.result.InsertOneResult
import uk.gov.hmrc.customs.common.logging.CdsLogger
import uk.gov.hmrc.customs.notification.receiver.models.{ConversationId, CsId, NotificationRequest, NotificationRequestRecord}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class NotificationRequestRecordRepo @Inject()(mongoComponent: MongoComponent, logger: CdsLogger)(implicit ec: ExecutionContext)
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
  def insertNotificationRequestRecord(notificationRequestRecord: NotificationRequestRecord): Future[Unit] = {
    //TODO
//    logger.debug(s"[conversationId=[${notificationRequestRecord.notification.conversationId}]" +
//      s"[clientSubscriptionId=[${notificationRequestRecord.notification.csId}] saving clientNotification: [${notificationRequestRecord.notification}]")

    val result: Future[InsertOneResult] = collection.insertOne(notificationRequestRecord).toFuture()
    result.map{ insertResult =>
      if(insertResult.wasAcknowledged()){
        ()
      } else{
        val errorMessage = s"Client Notification not saved for clientSubscriptionId ${notificationRequestRecord.notification.csId}"
        logger.error(errorMessage)
        throw new RuntimeException(errorMessage)
      }
    }
  }

  def findAllByCsId(csId: CsId): Future[Seq[NotificationRequest]] = {
    logger.debug(s"fetching clientNotification(s) with csid: [${csId}]")
    findAllWithFilterAndSort(buildCsIdFilter(csId))
  }

  def findAllByConversationId(conversationId: ConversationId): Future[Seq[NotificationRequest]] = {
    logger.debug(s"fetching clientNotification(s) with conversationId: [${conversationId}]")
    findAllWithFilterAndSort(buildConversationIdFilter(conversationId))
  }

  def countNotificationsByCsId(csId: CsId): Future[Int] = {
    logger.debug(s"counting clientNotification(s) with csid: [${csId}]")
    countNotificationsByFilter(buildCsIdFilter(csId))
  }

  def countNotificationsByConversationId(conversationId: ConversationId): Future[Int] = {
    logger.debug(s"counting clientNotification(s) with conversationId: [${conversationId}]")
    countNotificationsByFilter(buildConversationIdFilter(conversationId))
  }

  def countAllNotifications(): Future[Int] = {
    collection.countDocuments().toFuture().map(_.toInt)
  }

  def dropCollection(): Future[Unit] = {
    logger.debug("dropping the collection")
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
      sortNotificationRequestRecordsByDateAscending(notificationRequestRecords).map(_.notification)
    }
  }

  private def countNotificationsByFilter(filter: conversions.Bson): Future[Int] = {
    collection.countDocuments(filter).toFuture().map(_.toInt)
  }

  private def sortNotificationRequestRecordsByDateAscending(notificationRequestRecords: Seq[NotificationRequestRecord]): Seq[NotificationRequestRecord] = {
    notificationRequestRecords.sortWith((thisRecord, nextRecord) => thisRecord.timeReceived.isBefore(nextRecord.timeReceived))
  }
}
