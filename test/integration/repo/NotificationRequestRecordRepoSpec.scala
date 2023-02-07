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
import org.bson.types.ObjectId
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
import uk.gov.hmrc.customs.notification.receiver.models.{ConversationId, CsId, Header, TestChild, TestX}
import uk.gov.hmrc.customs.notification.receiver.repo.NotificationRequestRecordRepo
import util.{UnitSpec, WireMockSupport}
import util.TestData._

import java.util.UUID
import scala.collection.immutable.Seq
import scala.concurrent.Future

class NotificationRequestRecordRepoSpec extends ItSpec{
  private def collectionSize: Int = {
    //repository.dropDb()
    repository.countAllNotifications().futureValue
  }

  val csId1: CsId = CsId(UUID.fromString("ffff01f9-ec3b-4ede-b263-61b626dde232"))
  val csId2: CsId = CsId(UUID.fromString("ffff01f9-ec3b-4ede-b263-61b626dde239"))
  val csId3: CsId = CsId(UUID.fromString("ffff01f9-ec3b-4ede-b263-61b626dde234"))
  val testConversationId1: ConversationId = ConversationId(UUID.fromString("eaca01f9-ec3b-4ede-b263-61b626dde232"))
  val testConversationId2: ConversationId = ConversationId(UUID.fromString("eaca01f9-ec3b-4ede-b263-61b626dde239"))
  val testConversationId3: ConversationId = ConversationId(UUID.fromString("eaca01f9-ec3b-4ede-b263-61b626dde231"))
  val testHeader1: Header = Header(name = "testHeader1", value = "value1")
  val testHeader2: Header = Header(name = "testHeader2", value = "value2")
  val testHeader3: Header = Header(name = "testHeader3", value = "value3")
  val testHeader4: Header = Header(name = "testHeader4", value = "value4")
  val testHeader5: Header = Header(name = "testHeader5", value = "value5")
  val testHeader6: Header = Header(name = "testHeader6", value = "value6")
  //TODO revert to seq?
  val testHeaders1: List[Header] = List(testHeader1, testHeader2)
  val testHeaders2: List[Header] = List(testHeader3, testHeader4)
  val testHeaders3: List[Header] = List(testHeader5, testHeader6)
  lazy val testObjectId1: ObjectId = new ObjectId
  lazy val testObjectId2: ObjectId = new ObjectId
  lazy val testObjectId3: ObjectId = new ObjectId
  lazy val testAuthHeaderToken: String = "testAuthHeaderToken"
  lazy val testXmlPayload: String = "testXmlPayload"
  lazy val testChild1: TestChild = TestChild(
    csid = csId1,
    conversationId = testConversationId1,
    authHeaderToken = testAuthHeaderToken,
    outboundCallHeaders = testHeaders1,
    xmlPayload = testXmlPayload)
  lazy val testChild2: TestChild = TestChild(
    csid = csId2,
    conversationId = testConversationId2,
    authHeaderToken = testAuthHeaderToken,
    outboundCallHeaders = testHeaders2,
    xmlPayload = testXmlPayload)
  lazy val testChild3: TestChild = TestChild(
    csid = csId3,
    conversationId = testConversationId3,
    authHeaderToken = testAuthHeaderToken,
    outboundCallHeaders = testHeaders3,
    xmlPayload = testXmlPayload)
  val testX1: TestX = TestX(
    child = testChild1,
    //TODO make the time set
    timeReceived = DateTime.now().toDateTimeISO,
    _id = testObjectId1)
  val testX2: TestX = TestX(
    child = testChild2,
    //TODO make the time set
    timeReceived = DateTime.now().toDateTimeISO,
    _id = testObjectId2)
  val testX3: TestX = TestX(
    child = testChild3,
    //TODO make the time set
    timeReceived = DateTime.now().toDateTimeISO,
    _id = testObjectId3)

  private def upsertTestData: Future[Unit] = {
//    await(repository.upsertByCsid(CsidOne, ConversationIdOne, "1", testHeaders1, testObjectId1))
//    await(repository.upsertByCsid(CsidTwo, ConversationIdTwo, "2", testHeaders2, testObjectId2))
//    await(repository.upsertByCsid(CsidThree, ConversationIdThree, "3", testHeaders3, testObjectId3))
//    Future.successful()
    await(repository.upsertNotificationRequestRecordByCsId(testX1))
    await(repository.upsertNotificationRequestRecordByCsId(testX2))
    await(repository.upsertNotificationRequestRecordByCsId(testX3))
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

    val result1 = await(repository.findByCsid(csId1))
    result1.child shouldBe testChild1
    //TODO fix time issue below for all
    //result1.timeReceived shouldBe testX1.timeReceived
    result1._id shouldBe testObjectId1

    val result2 = await(repository.findByCsid(csId2))
    result2.child shouldBe testChild2
    result2._id shouldBe testObjectId2

    val result3 = await(repository.findByCsid(csId3))
    result3.child shouldBe testChild3
    result3._id shouldBe testObjectId3
  }

  "successfully find a specific notification by id2" in {
    await(upsertTestData)

    val result1 = await(repository.findByConversationId(testConversationId1))
    result1.child shouldBe testChild1
    result1._id shouldBe testObjectId1

    val result2 = await(repository.findByConversationId(testConversationId2))
    result2.child shouldBe testChild2
    result2._id shouldBe testObjectId2

    val result3 = await(repository.findByConversationId(testConversationId3))
    result3.child shouldBe testChild3
    result3._id shouldBe testObjectId3
  }

  "successfully find a random notification" in {
    await(upsertTestData)

    val result1 = await(repository.findAny)
    //TODO fix date generation above otherwise this will sometimes find another notification
    result1.child shouldBe testChild1
    result1._id shouldBe testObjectId1
  }

  //  "ensure indexes are created" in {
  //    repository.dropDb().futureValue
  //    repository.ensureIndexes.futureValue
  //    println(repository.collectionName)
  //    println(repository.collection.listIndexes().toFuture().futureValue.map(x => x))
  //    repository.collection.listIndexes().toFuture().futureValue.size shouldBe 3
  //  }

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
