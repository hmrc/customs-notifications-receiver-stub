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

import org.mongodb.scala.SingleObservable
import org.mongodb.scala.bson.BsonValue
import org.mongodb.scala.model.{FindOneAndUpdateOptions, IndexModel, IndexOptions}
import org.mongodb.scala.model.Indexes.{ascending, compoundIndex, descending}
import uk.gov.hmrc.mongo.MongoComponent

import javax.inject.{Inject, Singleton}
import org.mongodb.scala.model.Filters.{and, equal}
import play.api.libs.json.Json
import uk.gov.hmrc.mongo.play.json.{Codecs, PlayMongoRepository}
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.notification.receiver.logging.LoggingHelper._
import uk.gov.hmrc.customs.notification.receiver.models.{ConversationId, CsId, NotificationRequest, NotificationRequestRecord}

import scala.collection.immutable.Seq
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class MongoNotificationsRepo @Inject()(mongo: MongoComponent,
                                       logger: CdsLogger)
                                      (implicit ec: ExecutionContext)
  extends PlayMongoRepository[NotificationRequestRecord](
    collectionName = "notifications",
    mongoComponent = mongo,
    domainFormat = NotificationRequestRecord.format,
    indexes = Seq(
      IndexModel(
        compoundIndex(ascending("notification.csid"), descending("timeReceived")),
        IndexOptions()
          .name("csid-timeReceived-Index")
          .unique(false)
      ),
      IndexModel(
        compoundIndex(ascending("notification.conversationId"),descending("timeReceived")),
        IndexOptions()
          .name("conversationId-timeReceived-Index")
          .unique(false)
      ),
    )

  ) with NotificationRepo {

  def persist(n: NotificationRequest): Future[Boolean] = {
    logger.debug(s"${logMsgPrefix(n)} saving clientNotification: $n")
   //TODO remove print lines
    println(s"$n")
    val record = NotificationRequestRecord(n)
    println(s"$record")
    val selector = equal("_id" ,  record.id)
    println(s"$selector")
    val update = and(equal("$currentDate" , ("timeReceived" , true)), equal( "$set" , record))
    println(s"$update")
    collection.findOneAndUpdate(selector, update, options= FindOneAndUpdateOptions().upsert(true)).toFutureOption().map {
      case Some(_: NotificationRequestRecord) => true
      case None =>
        val errorLogMsg = s"Client Notification not saved for clientSubscriptionId ${n.csid}"
        handleError(new RuntimeException(errorLogMsg),errorLogMsg)
    }.recoverWith {
      case e =>
        val errorMsg = s"Client Notification not saved for clientSubscriptionId ${n.csid}"
        logger.error(errorMsg)
        Future.failed(e)
    }

  }

  def jsonToBson(json: (String, Json.JsValueWrapper)*): BsonValue = {
    Codecs.toBson(Json.obj(json: _*))
  }

  def notificationsByCsId(csid: CsId): Future[Seq[NotificationRequest]] =
  {
    logger.debug(s"fetching clientNotification(s) with csid: ${csid.toString}")
    val selector = equal("notification.csid", csid)
    val sortOrder = ascending("timeReceived")
    val aggregate = collection.aggregate[NotificationRequest](Seq(selector,sortOrder)).toFuture()
    aggregate.map(_.toList)

  }

  def notificationsByConversationId(conversationId: ConversationId): Future[Seq[NotificationRequest]] =
  {
    logger.debug(s"fetching clientNotification(s) with conversationId: ${conversationId.toString}")
    val selector = equal("notification.conversationId" , Codecs.toBson(conversationId))
    val sortOrder = ascending("timeReceived")
    val aggregate = collection.aggregate[NotificationRequest](Seq(selector, sortOrder)).toFuture()
    aggregate.map(_.toList)
  }

  def notificationCountByCsId(csid: CsId): Future[Int] =
  {
    logger.debug(s"counting clientNotification(s) with csid: ${csid.toString}")
    val selector = equal("notification.csid" , Codecs.toBson(csid))
    collection.countDocuments(selector).asInstanceOf[SingleObservable[Int]].toFuture()
  }

  def notificationCountByConversationId(conversationId: ConversationId): Future[Int] =
  {
    logger.debug(s"counting clientNotification(s) with conversationId: ${conversationId.toString}")
    val selector = equal("notification.conversationId" , Codecs.toBson(conversationId))
    collection.countDocuments(selector).asInstanceOf[SingleObservable[Int]].toFuture()
  }

  def notificationCount: Future[Int] =
  {
    logger.debug("counting all clientNotifications")
    collection.countDocuments().asInstanceOf[SingleObservable[Int]].toFuture()
  }

  def handleError(e: Exception, errorLogMessage: String): Nothing = {
    lazy val errorMsg = errorLogMessage + s"\n ${e.getMessage}"
    logger.error(errorMsg)
    throw new RuntimeException(errorMsg)
  }

  def clearAll(): Future[Unit] = {
    collection.drop().toFuture().map{isOk =>
      logger.debug(s"clear all result=$isOk")
      ()
    }
  }
}
