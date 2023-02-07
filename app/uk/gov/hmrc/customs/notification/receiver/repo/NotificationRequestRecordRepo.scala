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
import org.mongodb.scala.FindObservable
import org.mongodb.scala.model.{FindOneAndReplaceOptions, FindOneAndUpdateOptions, IndexModel, IndexOptions, ReplaceOptions, Updates}
import org.mongodb.scala.model.Indexes.compoundIndex
import uk.gov.hmrc.customs.notification.receiver.models.{ConversationId, CsId, NotificationRequest, NotificationRequestRecord, TestChild, TestX}
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
      Codecs.playFormatCodec(TestChild.format)
    ),
    indexes = Seq(
      IndexModel(ascending("child.csid")),
      IndexModel(ascending("child.conversationId"))
    )
//    indexes = Seq(
//      IndexModel(
//        compoundIndex(ascending("notification.csid"), descending("timeReceived")),
//        IndexOptions()
//          .name("csid-timeReceived-Index")
//          .unique(false)
//      ),
//      IndexModel(
//        compoundIndex(ascending("notification.conversationId"), descending("timeReceived")),
//        IndexOptions()
//          .name("conversationId-timeReceived-Index")
//          .unique(false)))
  ) {

  def upsertById1(id1: String, id2: String, value: String): Future[Unit] = {
    val filter: Bson = equal("child.csid", id1)
    val dataObject: TestX = TestX(
      id1 = id1,
      id2 = id2,
      value = value,
      child = TestChild(
        csid = id1,
        conversationId = id2,
        childValue = "LOL"),
      _id = new ObjectId())

    collection.findOneAndReplace(
      filter = filter,
      replacement = dataObject,
      options = FindOneAndReplaceOptions().upsert(true)).toFuture().map(_ => ())
  }

  def findById1(id1: String): Future[TestX] = {
    val filter: Bson = equal("child.csid", id1)
    val x: FindObservable[TestX] = collection.find(filter)
    val y: Future[Seq[TestX]] = x.toFuture()
    val z: Future[TestX] = y.map(_.toList.head)
    z
  }

  def findById2(id2: String): Future[TestX] = {
    val filter: Bson = equal("child.conversationId", id2)
    val x: FindObservable[TestX] = collection.find(filter)
    val y: Future[Seq[TestX]] = x.toFuture()
    val z: Future[TestX] = y.map(_.toList.head)
    z
  }

  def findAny: Future[TestX] = {
    val x: FindObservable[TestX] = collection.find()
    val y: Future[Seq[TestX]] = x.toFuture()
    val z: Future[TestX] = y.map(_.toList.head)
    z
  }


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
