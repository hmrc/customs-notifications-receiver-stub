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

import com.codahale.metrics.SharedMetricRegistries
import com.google.inject.AbstractModule
import org.joda.time.{DateTime, DateTimeZone, Seconds}
import org.scalatest.concurrent.ScalaFutures.convertScalaFuture
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.Application
import play.api.inject.Injector
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.test.Helpers
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import support.ItSpec
import uk.gov.hmrc.customs.notification.receiver.models.{TestChild, TestX}
import uk.gov.hmrc.customs.notification.receiver.repo.NotificationRequestRecordRepo
import util.{UnitSpec, WireMockSupport}
import util.TestData._

import scala.collection.immutable.Seq

class NotificationRequestRecordRepoSpec extends ItSpec{
  private def collectionSize: Int = {
    //repository.dropDb()
    repository.countAllNotifications().futureValue
  }

    "count should be 0 with an empty repo" in {
      collectionSize shouldBe 0
    }

//  "ensure indexes are created" in {
//    repository.dropDb().futureValue
//    repository.ensureIndexes.futureValue
//    println(repository.collectionName)
//    println(repository.collection.listIndexes().toFuture().futureValue.map(x => x))
//    repository.collection.listIndexes().toFuture().futureValue.size shouldBe 3
//  }

  "successfully save multiple notifications" in {
    await(repository.upsertById1("A","a", "1"))
    await(repository.upsertById1("B", "b", "2"))
    await(repository.upsertById1("C", "c", "3"))

    collectionSize shouldBe 3
  }

  "successfully find a specific notification by id1" in {
    await(repository.upsertById1("A", "a", "1"))
    await(repository.upsertById1("B", "b", "2"))
    await(repository.upsertById1("C", "c", "3"))

    val findResult = await(repository.findById1("A"))

    findResult.id1 shouldBe "A"
    findResult.id2 shouldBe "a"
    findResult.value shouldBe "1"
    findResult.child shouldBe TestChild("LOL")

    val findResult2 = await(repository.findById1("C"))

    findResult2.id1 shouldBe "C"
    findResult2.id2 shouldBe "c"
    findResult2.value shouldBe "3"
    findResult2.child shouldBe TestChild("LOL")
  }

  "successfully find a specific notification by id2" in {
    await(repository.upsertById1("A", "a", "1"))
    await(repository.upsertById1("B", "b", "2"))
    await(repository.upsertById1("C", "c", "3"))

    val findResult = await(repository.findById2("a"))

    findResult.id1 shouldBe "A"
    findResult.id2 shouldBe "a"
    findResult.value shouldBe "1"
    findResult.child shouldBe TestChild("LOL")

    val findResult2 = await(repository.findById2("c"))

    findResult2.id1 shouldBe "C"
    findResult2.id2 shouldBe "c"
    findResult2.value shouldBe "3"
    findResult2.child shouldBe TestChild("LOL")
  }

  "successfully find a random notification" in {
    await(repository.upsertById1("A", "a", "1"))
    await(repository.upsertById1("B", "b", "2"))
    await(repository.upsertById1("C", "c", "3"))
    val findResult = await(repository.findAny)

    findResult.id1 shouldBe "A"
    findResult.id2 shouldBe "a"
    findResult.value shouldBe "1"
    findResult.child shouldBe TestChild("LOL")
  }

//    "successfully save a single notification" in {
//  "successfully find a random notification" in {
//    await(repository.upsertById1("A", "a", "1"))
//    await(repository.upsertById1("B", "b", "2"))
//    await(repository.upsertById1("C", "c", "3"))
//    val findResult = await(repository.findAny)
//
//    findResult shouldBe TestX("A","a", "1", TestChild("LOL"))

      //when(mockErrorHandler.handleSavxeError(any(), any(), any())).thenReturn(true)
     // repository.insert(NotificationRequestOne)
//      await(repository.insertString("1"))
//      await(repository.insertString("2"))



//      repository.upsertString("1")
//      repository.upsertString("2")
//      repository.upsertString("3")

      //repository.upsertString("4")
      //repository.upsertString("5")

      //val findResult = repository.findById("TestX")


     // val findResult = repository

      //repository.upsert(NotificationRequestOne).futureValue

//      val logMsg = "[conversationId=eaca01f9-ec3b-4ede-b263-61b626dde232][clientSubscriptionId=ffff01f9-ec3b-4ede-b263-61b626dde232] saving clientNotification: NotificationRequest(ffff01f9-ec3b-4ede-b263-61b626dde232,eaca01f9-ec3b-4ede-b263-61b626dde232,AUTH_TOKEN,List(),<foo>OneOfTwo</foo>)"
//     // logVerifier("debug", logMsg)
//      //saveResult shouldBe true
//      collectionSize shouldBe 1
//      val findResult = repository.findNotificationsByCsId(CsidOne).futureValue.head
//      findResult.id should not be None
//      findResult.timeReceived should not be None
//      Seconds.secondsBetween(DateTime.now(DateTimeZone.UTC), findResult.timeReceived.get).getSeconds should be < 3
//      findResult.notification shouldBe NotificationRequestOne
   // }
//
//    "successfully save when called multiple times" in {
//
//      repository.upsert(NotificationRequestOne).futureValue
//      repository.upsert(NotificationRequestOne).futureValue
//
//      collectionSize shouldBe 2
//      val clientNotifications = repository.findNotificationsByCsId(CsidOne).futureValue
//      clientNotifications.size shouldBe 2
//      clientNotifications.head.id should not be None
//    }
//
//    "find by CsId" in {
//
//      repository.upsert(NotificationRequestOne).futureValue
//      repository.upsert(NotificationRequestOneTwo).futureValue
//      repository.upsert(NotificationRequestTwo).futureValue
//
//      repository.findNotificationsByCsId(CsidOne).futureValue shouldBe Seq(NotificationRequestOne, NotificationRequestOneTwo)
//      //logVerifier("debug", "fetching clientNotification(s) with csid: ffff01f9-ec3b-4ede-b263-61b626dde232")
//      repository.findNotificationsByCsId(CsidTwo).futureValue shouldBe Seq(NotificationRequestTwo)
//    }
//
//    "find by ConversationId" in {
//
//      repository.upsert(NotificationRequestOne).futureValue
//      repository.upsert(NotificationRequestOneTwo).futureValue
//      repository.upsert(NotificationRequestTwo).futureValue
//
//      repository.findNotificationsByConversationId(ConversationIdOne).futureValue shouldBe Seq(NotificationRequestOne, NotificationRequestOneTwo)
//      //logVerifier("debug", "fetching clientNotification(s) with conversationId: eaca01f9-ec3b-4ede-b263-61b626dde232")
//      repository.findNotificationsByConversationId(ConversationIdTwo).futureValue shouldBe Seq(NotificationRequestTwo)
//    }
//
//    "count by CsId" in {
//
//      repository.upsert(NotificationRequestOne).futureValue
//      repository.upsert(NotificationRequestOne).futureValue
//      repository.upsert(NotificationRequestTwo).futureValue
//
//      repository.countNotificationsByCsId(CsidOne).futureValue shouldBe 2
//      //logVerifier("debug", "counting clientNotification(s) with csid: ffff01f9-ec3b-4ede-b263-61b626dde232")
//      repository.countNotificationsByCsId(CsidTwo).futureValue shouldBe 1
//    }
//
//    "count by conversationId" in {
//
//      repository.upsert(NotificationRequestOne).futureValue
//      repository.upsert(NotificationRequestOne).futureValue
//      repository.upsert(NotificationRequestTwo).futureValue
//
//      repository.countNotificationsByConversationId(ConversationIdOne).futureValue shouldBe 2
//     // logVerifier("debug", "counting clientNotification(s) with conversationId: eaca01f9-ec3b-4ede-b263-61b626dde232")
//      repository.countNotificationsByConversationId(ConversationIdTwo).futureValue shouldBe 1
//    }
//
//    "count all notifications" in {
//
//      repository.upsert(NotificationRequestOne).futureValue
//      repository.upsert(NotificationRequestOne).futureValue
//      repository.upsert(NotificationRequestTwo).futureValue
//
//      repository.countAllNotifications().futureValue shouldBe 3
//      //logVerifier("debug", "counting all clientNotifications")
//    }
//
//    "delete" in {
//
//      repository.upsert(NotificationRequestOne).futureValue
//      repository.upsert(NotificationRequestOne).futureValue
//      repository.countNotificationsByCsId(CsidOne).futureValue shouldBe 2
//
//      repository.dropDb().futureValue
//
//      //logVerifier("debug", "clear all result=true")
//      val count = repository.countNotificationsByCsId(CsidOne).futureValue
//      count shouldBe 0
//    }
}
