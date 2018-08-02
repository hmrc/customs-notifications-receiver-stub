/*
 * Copyright 2018 HM Revenue & Customs
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

import org.joda.time.{DateTime, DateTimeZone, Seconds}
import org.mockito.ArgumentMatchers._
import org.mockito.Mockito._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import play.api.libs.json.Json
import reactivemongo.api.{Cursor, DB}
import reactivemongo.play.json.JsObjectDocumentWriter
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.notification.receiver.models.{CsId, NotificationRequestRecord}
import uk.gov.hmrc.customs.notification.receiver.repo.{MongoDbProvider, MongoNotificationsRepo, NotificationRepositoryErrorHandler}
import uk.gov.hmrc.mongo.MongoSpecSupport
import uk.gov.hmrc.play.test.UnitSpec
import util.MockitoPassByNameHelper.PassByNameVerifier
import util.TestData._

import scala.concurrent.ExecutionContext.Implicits.global

class ClientNotificationMongoRepoSpec extends UnitSpec
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with MockitoSugar
  with MongoSpecSupport  { self =>

  private val mockLogger = mock[CdsLogger]
  private val mockErrorHandler = mock[NotificationRepositoryErrorHandler]

  private val mongoDbProvider = new MongoDbProvider {
    override val mongo: () => DB = self.mongo
  }

  private val repository = new MongoNotificationsRepo(mongoDbProvider, mockErrorHandler, mockLogger)

  override def beforeEach() {
    await(repository.drop)
  }

  override def afterAll() {
    await(repository.drop)
  }

  private def collectionSize: Int = {
    await(repository.collection.count())
  }

  private def selector(clientSubscriptionId: CsId) = {
    Json.obj("notification.csid" -> clientSubscriptionId)
  }

  private def logVerifier(logLevel: String, logText: String) = {
    PassByNameVerifier(mockLogger, logLevel)
      .withByNameParam(logText)
      .verify()
  }

  "repository" should {
    "successfully save a single notification" in {
      when(mockErrorHandler.handleSaveError(any(), any(), any())).thenReturn(true)

      val saveResult = await(repository.persist(NotificationRequestOne))

      val logMsg = "[conversationId=eaca01f9-ec3b-4ede-b263-61b626dde232][clientSubscriptionId=ffff01f9-ec3b-4ede-b263-61b626dde232] saving clientNotification: NotificationRequest(ffff01f9-ec3b-4ede-b263-61b626dde232,eaca01f9-ec3b-4ede-b263-61b626dde232,AUTH_TOKEN,List(),<foo>OneOfTwo</foo>)"
      logVerifier("debug", logMsg)
      saveResult shouldBe true
      collectionSize shouldBe 1
      val findResult = await(repository.collection.find(selector(CsidOne)).one[NotificationRequestRecord]).get
      findResult.id should not be None
      findResult.timeReceived should not be None
      Seconds.secondsBetween(DateTime.now(DateTimeZone.UTC), findResult.timeReceived.get).getSeconds should be < 3
      findResult.notification shouldBe NotificationRequestOne
    }

    "successfully save when called multiple times" in {
      await(repository.persist(NotificationRequestOne))
      await(repository.persist(NotificationRequestOne))

      collectionSize shouldBe 2
      val clientNotifications = await(repository.collection.find(selector(CsidOne)).cursor[NotificationRequestRecord]().collect[Seq](Int.MaxValue, Cursor.FailOnError[Seq[NotificationRequestRecord]]()))
      clientNotifications.size shouldBe 2
      clientNotifications.head.id should not be None
    }

    "find by CsId" in {
      await(repository.persist(NotificationRequestOne))
      await(repository.persist(NotificationRequestOneTwo))
      await(repository.persist(NotificationRequestTwo))

      await(repository.notificationsByCsId(CsidOne)) shouldBe Seq(NotificationRequestOne, NotificationRequestOneTwo)
      logVerifier("debug", "fetching clientNotification(s) with csid: ffff01f9-ec3b-4ede-b263-61b626dde232")
      await(repository.notificationsByCsId(CsidTwo)) shouldBe Seq(NotificationRequestTwo)
    }

    "count by CsId" in {
      await(repository.persist(NotificationRequestOne))
      await(repository.persist(NotificationRequestOne))
      await(repository.persist(NotificationRequestTwo))

      await(repository.notificationCountByCsId(CsidOne)) shouldBe 2
      logVerifier("debug", "counting clientNotification(s) with csid: ffff01f9-ec3b-4ede-b263-61b626dde232")
      await(repository.notificationCountByCsId(CsidTwo)) shouldBe 1
    }

    "delete" in {
      await(repository.persist(NotificationRequestOne))
      await(repository.persist(NotificationRequestOne))
      await(repository.notificationCountByCsId(CsidOne)) shouldBe 2

      await(repository.clearAll())

      logVerifier("debug", "clear all result=true")
      val count = await(repository.notificationCountByCsId(CsidOne))
      count shouldBe 0
    }
  }
}
