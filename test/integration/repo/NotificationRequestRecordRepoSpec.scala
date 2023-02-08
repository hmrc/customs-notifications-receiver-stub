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

import play.api.test.Helpers.{await, defaultAwaitTimeout}
import support.ItSpec
import util.TestData._
import scala.concurrent.Future

class NotificationRequestRecordRepoSpec extends ItSpec{

  private def upsertTestData: Future[Unit] = {
    await(repository.insertNotificationRequestRecord(notificationRequestRecord1))
    await(repository.insertNotificationRequestRecord(notificationRequestRecord2))
    await(repository.insertNotificationRequestRecord(notificationRequestRecord3))
    //TODO remove below
    Future.successful()
  }

  "count should be 0 with an empty repo" in {
    collectionSize shouldBe 0
  }

  "successfully save multiple notifications" in {
    await(upsertTestData)

    collectionSize shouldBe 3
  }

  "successfully find a specific notification by id1" in {
    await(upsertTestData)

    val result1 = await(repository.findByCsId(csId1))
    result1.notification shouldBe notificationRequest1
    //TODO fix time issue below for all
    //result1.timeReceived shouldBe testX1.timeReceived
    result1._id shouldBe objectId1

    val result2 = await(repository.findByCsId(csId2))
    result2.notification shouldBe notificationRequest2
    result2._id shouldBe objectId2

    val result3 = await(repository.findByCsId(csId3))
    result3.notification shouldBe notificationRequest3
    result3._id shouldBe objectId3
  }

  "successfully find a specific notification by id2" in {
    await(upsertTestData)

    val result1 = await(repository.findByConversationId(conversationId1))
    result1.notification shouldBe notificationRequest1
    result1._id shouldBe objectId1

    val result2 = await(repository.findByConversationId(conversationId2))
    result2.notification shouldBe notificationRequest2
    result2._id shouldBe objectId2

    val result3 = await(repository.findByConversationId(conversationId3))
    result3.notification shouldBe notificationRequest3
    result3._id shouldBe objectId3
  }

  "successfully find a random notification" in {
    await(upsertTestData)

    val result1 = await(repository.findAny)
    //TODO fix date generation above otherwise this will sometimes find another notification
    result1.notification shouldBe notificationRequest1
    result1._id shouldBe objectId1
  }

  "count notifications by CsId" in {
    await(upsertTestData)

    val result = await(repository.countNotificationsByCsId(csId1))
    result shouldBe 1
  }

  "count notifications by ConversationId" in {
    await(upsertTestData)

    val result = await(repository.countNotificationsByConversationId(conversationId1))
    result shouldBe 1
  }

  private def collectionSize: Int = {
    repository.countAllNotifications().futureValue
  }
}
