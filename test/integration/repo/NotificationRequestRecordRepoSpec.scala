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

class NotificationRequestRecordRepoSpec extends ItSpec{

  "count should be 0 with an empty repo" in {
    collectionSize shouldBe 0
  }

  "save multiple notifications" in {
    insertTestData

    collectionSize shouldBe 6
  }

  "find all notifications by CsId when one exists" in {
    insertTestDataNoDuplicateCsOrConversationIds

    val result = await(repository.findAllByCsId(csId1))
    result shouldBe Seq(notificationRequest1)
  }

  "find all notifications by CsId when multiple exist" in {
    insertTestData

    val result = await(repository.findAllByCsId(csId1))
    result shouldBe Seq(notificationRequest1, notificationRequest1)
  }

  "find all notifications by ConversationId when one exists" in {
    insertTestDataNoDuplicateCsOrConversationIds

    val result = await(repository.findAllByConversationId(conversationId1))
    result shouldBe Seq(notificationRequest1)
  }

  "find all notifications by ConversationId when multiple exist" in {
    insertTestData

    val result = await(repository.findAllByConversationId(conversationId1))
    result shouldBe Seq(notificationRequest1, notificationRequest1)
  }

  "successfully find a specific notification by id1" in {
    insertTestData

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
    insertTestData

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
    insertTestData

    val result1 = await(repository.findAny)
    //TODO fix date generation above otherwise this will sometimes find another notification
    result1.notification shouldBe notificationRequest1
    result1._id shouldBe objectId1
  }

  "count notifications by CsId" in {
    insertTestData

    val result = await(repository.countNotificationsByCsId(csId1))
    result shouldBe 2
  }

  "count notifications by ConversationId" in {
    insertTestData

    val result = await(repository.countNotificationsByConversationId(conversationId1))
    result shouldBe 2
  }

  private def insertTestData(): Unit = {
    await(repository.insertNotificationRequestRecord(notificationRequestRecord1))
    await(repository.insertNotificationRequestRecord(notificationRequestRecord2))
    await(repository.insertNotificationRequestRecord(notificationRequestRecord3))
    await(repository.insertNotificationRequestRecord(notificationRequestRecord4))
    await(repository.insertNotificationRequestRecord(notificationRequestRecord5))
    await(repository.insertNotificationRequestRecord(notificationRequestRecord6))
  }

  private def insertTestDataNoDuplicateCsOrConversationIds(): Unit = {
    await(repository.insertNotificationRequestRecord(notificationRequestRecord1))
    await(repository.insertNotificationRequestRecord(notificationRequestRecord2))
    await(repository.insertNotificationRequestRecord(notificationRequestRecord3))
  }

  private def collectionSize(): Int = {
    repository.countAllNotifications().futureValue
  }
}
