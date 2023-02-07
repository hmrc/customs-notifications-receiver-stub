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
import org.mongodb.scala.FindObservable
import org.mongodb.scala.model.{FindOneAndReplaceOptions, FindOneAndUpdateOptions, IndexModel, IndexOptions, ReplaceOptions, Updates}
import org.mongodb.scala.model.Indexes.compoundIndex
import uk.gov.hmrc.customs.notification.receiver.models.{ConversationId, CsId, Header, NotificationRequest, NotificationRequestRecord, TestX}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}
import org.mongodb.scala.model.Indexes.{ascending, descending}
import org.mongodb.scala.bson.conversions
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters.equal
import org.mongodb.scala.model.Updates.{combine, currentDate, set}
import play.api.libs.json.{Format, Json}
import uk.gov.hmrc.mongo.play.json.Codecs.JsonOps
import uk.gov.hmrc.mongo.play.json.formats.MongoFormats
import uk.gov.hmrc.mongo.play.json.formats.MongoUuidFormats.Implicits._
import uk.gov.hmrc.mongo.play.json.formats.MongoLegacyUuidFormats.Implicits._
import uk.gov.hmrc.customs.notification.receiver.models.TestX.objectIdFormat

import scala.concurrent.{ExecutionContext, Future}
import javax.inject.{Inject, Singleton}


@Singleton
class NotificationRequestRecordRepo @Inject()(mongoComponent: MongoComponent)(implicit ec: ExecutionContext)
  //extends PlayMongoRepository[NotificationRequestRecord](
  extends PlayMongoRepository[TestX](
    mongoComponent = mongoComponent,
    collectionName = "notifications",
    domainFormat = TestX.format,
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
  def upsertNotificationRequestRecordByCsId(notificationRequestRecord: TestX): Future[Unit] = {
    val filter: Bson = equal("notification.csid", notificationRequestRecord.notification.csid)

    collection.findOneAndReplace(
      filter = filter,
      replacement = notificationRequestRecord,
      options = FindOneAndReplaceOptions().upsert(true)).toFuture().map(_ => ())
  }

  def findByCsid(csid: CsId): Future[TestX] = {
    val filter: Bson = equal("notification.csid", csid)

    collection.find(filter).toFuture().map(_.toList.head)
  }

  def findByConversationId(conversationId: ConversationId): Future[TestX] = {
    val filter: Bson = equal("notification.conversationId", conversationId)

    collection.find(filter).toFuture().map(_.toList.head)
  }

  def findAny: Future[TestX] = {
    collection.find().toFuture().map(_.toList.head)
  }


//  def upsertByCsid(csid: CsId, conversationId: ConversationId, timeReceived: String, outboundCallHeaders: Seq[Header], objectId: ObjectId): Future[Unit] = {
//    val filter: Bson = equal("child.csid", csid)
//    val dataObject: TestX = TestX(
//      child = TestChild(
//        csid = csid,
//        conversationId = conversationId,
//        authHeaderToken = "AHT",
//        outboundCallHeaders = outboundCallHeaders.toList,
//        xmlPayload = "XPL"),
//      timeReceived = DateTime.now().toDateTimeISO,
//      _id = objectId)
//
//    collection.findOneAndReplace(
//      filter = filter,
//      replacement = dataObject,
//      options = FindOneAndReplaceOptions().upsert(true)).toFuture().map(_ => ())
//  }

//  def insert(notificationRequest: NotificationRequest): Future[Unit] = {
//    val notificationRequestRecord: NotificationRequestRecord = NotificationRequestRecord(
//      notification = notificationRequest)
//
//    val p  = notificationRequestRecord
//    println("QQQQQ" + p)
//
//    collection.insertOne(TestX("hello")).toFuture.map(_ => ())
//
//    //collection.insertOne(notificationRequestRecord).toFuture().map(_ => ())
//  }
//
//  def upsert(notificationRequest: NotificationRequest): Future[Unit] = {
//    val notificationRequestRecord: NotificationRequestRecord = NotificationRequestRecord(notificationRequest)
//    val selector: Bson = equal("_id", notificationRequestRecord.id)
//    //val update: Bson = Updates.combine(currentDate("timeReceived"), set("notification", notificationRequestRecord))
//    val update = notificationRequestRecord
//    //val update = Updates.set()
//    //collection.findOneAndUpdate(selector, update, options= FindOneAndUpdateOptions().upsert(true)).toFuture().map(_ => ())
//    collection.replaceOne(
//      filter = equal("_id", notificationRequestRecord.id),
//      replacement = notificationRequestRecord,
//      options = ReplaceOptions().upsert(true)).toFuture.map(_ => ())
//  }
//
//  def findNotificationsByCsId(csId: CsId): Future[Seq[NotificationRequestRecord]] = {
//    findNotificationsByFilter(buildCsIdFilter(csId))
//  }
//
//  def findNotificationsByConversationId(conversationId: ConversationId): Future[Seq[NotificationRequestRecord]] = {
//    findNotificationsByFilter(buildConversationIdFilter(conversationId))
//  }
//
//  def countNotificationsByCsId(csId: CsId): Future[Int] = {
//    countNotificationsByFilter(buildCsIdFilter(csId))
//  }
//
//  def countNotificationsByConversationId(conversationId: ConversationId): Future[Int] = {
//    countNotificationsByFilter(buildConversationIdFilter(conversationId))
//  }
//
  def countAllNotifications(): Future[Int] = {
    collection.countDocuments().toFuture().map(_.toInt)
  }
//
  def dropDb(): Future[Unit] = {
    collection.drop().toFuture().map(_ => ())
  }
//
//  private def buildCsIdFilter(csId: CsId): conversions.Bson = {
//    equal("notificationcsid", csId)
//  }
//
//  private def buildConversationIdFilter(conversationId: ConversationId): conversions.Bson = {
//    equal("conversationId", conversationId)
//  }
//
//  private def findNotificationsByFilter(filter: conversions.Bson): Future[Seq[NotificationRequestRecord]] = {
//    collection.find(filter).toFuture().map(sortNotificationRequestRecordsByDateAscending(_))
//  }
//
//  private def sortNotificationRequestRecordsByDateAscending(notificationRequestRecords: Seq[NotificationRequestRecord]): Seq[NotificationRequestRecord] = {
//    notificationRequestRecords.sortWith((thisRecord, nextRecord) =>
//      thisRecord.timeReceived.getOrElse(throw new RuntimeException("Error(sortNotificationRequestRecordsByDateAscending): timeReceived(1) is missing"))
//        .isBefore(nextRecord.timeReceived.getOrElse(throw new RuntimeException("Error(sortNotificationRequestRecordsByDateAscending): timeReceived(2) is missing")))
//    )
//  }
//
//  private def countNotificationsByFilter(filter: conversions.Bson): Future[Int] = {
//    collection.countDocuments(filter).toFuture().map(_.toInt)
//  }
}
