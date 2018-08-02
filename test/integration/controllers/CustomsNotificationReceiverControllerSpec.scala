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

package integration.controllers

import com.google.inject.AbstractModule
import integration.repo.InMemoryPersistenceService
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.Application
import play.api.inject.guice.GuiceApplicationBuilder
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test._
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.customs.notification.receiver.models.{ConversationId, CsId, CustomHeaderNames}
import uk.gov.hmrc.customs.notification.receiver.repo.NotificationRepo
import util.TestData._

import scala.concurrent.Future

class CustomsNotificationReceiverControllerSpec
  extends PlaySpec
  with BeforeAndAfterAll
  with BeforeAndAfterEach
  with GuiceOneAppPerTest
  {
  override def fakeApplication(): Application =
    GuiceApplicationBuilder()
      .overrides(new PersistenceModule())
      .build()

  private class PersistenceModule() extends AbstractModule {
    def configure() {
      bind(classOf[NotificationRepo]).to(classOf[InMemoryPersistenceService])
    }
  }

  "CustomsNotificationReceiverController" can {
    "In happy path" should {

      "return empty list for a client subscription Id that has no notifications" in {
        val eventualResult: Future[Result] = route(app, FakeRequest(GET, "/customs-notifications-receiver-stub/pushnotifications/" + CsidOne).withHeaders(AUTHORIZATION -> ("Basic " + CsidOne))).get

        status(eventualResult) mustBe OK
        contentType(eventualResult) mustBe Some("application/json")
        contentAsJson(eventualResult) mustBe Json.parse("[]")
      }

      "return 200 response with a payload representing the inserted notification for a valid Post" in {
        val eventualResult: Future[Result] = validPost(CsidOne, ConversationIdOne)

        status(eventualResult) mustBe OK
        contentType(eventualResult) mustBe Some("application/json")
        contentAsJson(eventualResult) mustBe notificationRequestJson(CsidOne, ConversationIdOne)
      }

      "return all received requests for a client subscription Id" in {
        awaitValidPost(CsidOne, ConversationIdOne)
        awaitValidPost(CsidOne, ConversationIdOne)
        awaitValidPost(CsidTwo, ConversationIdTwo)

        val eventualResult: Future[Result] = route(app, FakeRequest(GET, "/customs-notifications-receiver-stub/pushnotifications/" + CsidOne)).get

        status(eventualResult) mustBe OK
        contentType(eventualResult) mustBe Some("application/json")
        contentAsJson(eventualResult) mustBe notificationsResultJson(
          notificationRequest(CsidOne, ConversationIdOne),
          notificationRequest(CsidOne, ConversationIdOne)
        )

        val eventualResult2: Future[Result] = route(app, FakeRequest(GET, "/customs-notifications-receiver-stub/pushnotifications/" + CsidTwo)).get

        status(eventualResult2) mustBe OK
        contentType(eventualResult2) mustBe Some("application/json")
        contentAsJson(eventualResult2) mustBe notificationsResultJson(
          notificationRequest(CsidTwo, ConversationIdTwo)
        )

        val eventualResult3: Future[Result] = route(app, FakeRequest(GET, "/customs-notifications-receiver-stub/count/" + CsidOne)).get

        status(eventualResult3) mustBe OK
        contentType(eventualResult3) mustBe Some("application/json")
        contentAsJson(eventualResult3) mustBe Json.parse("""{"count": "2"}""")

        val eventualResult4: Future[Result] = route(app, FakeRequest(GET, "/customs-notifications-receiver-stub/count/" + CsidTwo)).get

        status(eventualResult4) mustBe OK
        contentType(eventualResult4) mustBe Some("application/json")
        contentAsJson(eventualResult4) mustBe Json.parse("""{"count": "1"}""")

      }

      "return NoContent when DELETE sent to pushNotifications endpoint" in {
        val eventualResult = route(app, FakeRequest(DELETE, "/customs-notifications-receiver-stub/pushnotifications").withXmlBody(XmlPayload)
          .withHeaders(
            AUTHORIZATION -> CsidOne.toString,
            CONTENT_TYPE -> MimeTypes.XML,
            ACCEPT -> MimeTypes.XML,
            USER_AGENT -> "Customs Declaration Service"
          )).get

        status(eventualResult) mustBe NO_CONTENT
      }

      "return empty received requests for a client subscription Id that had notifications but clear called" in {
          awaitValidPost(CsidOne, ConversationIdOne)
          awaitValidPost(CsidOne, ConversationIdOne)

        val eventualResult: Future[Result] = route(app, FakeRequest(GET, "/customs-notifications-receiver-stub/pushnotifications/" + CsidOne)).get

        status(eventualResult) mustBe OK
        contentType(eventualResult) mustBe Some("application/json")
        contentAsJson(eventualResult) mustBe notificationsResultJson(
          notificationRequest(CsidOne, ConversationIdOne),
          notificationRequest(CsidOne, ConversationIdOne))

        val eventualResult2 = route(app, FakeRequest(DELETE, "/customs-notifications-receiver-stub/pushnotifications").withXmlBody(XmlPayload)
          .withHeaders(
            AUTHORIZATION -> CsidOne.toString,
            CONTENT_TYPE -> MimeTypes.XML,
            ACCEPT -> MimeTypes.XML,
            USER_AGENT -> "Customs Declaration Service"
          )).get

        status(eventualResult2) mustBe NO_CONTENT

        val eventualResult3: Future[Result] = route(app, FakeRequest(GET, "/customs-notifications-receiver-stub/pushnotifications/" + CsidOne).withHeaders(AUTHORIZATION -> ("Basic " + CsidOne))).get

        status(eventualResult3) mustBe OK
        contentType(eventualResult3) mustBe Some("application/json")
        contentAsJson(eventualResult3) mustBe Json.parse("[]")
      }
    }

    "In un happy path" should {
      "return 400 for POST of notification when Authorisation header is missing" in {
        val eventualResult: Future[Result] = route(app, FakeRequest(POST, "/customs-notifications-receiver-stub/pushnotifications")
          .withXmlBody(XmlPayload)
          .withHeaders(
            CONTENT_TYPE -> MimeTypes.XML,
            ACCEPT -> MimeTypes.XML,
            USER_AGENT -> "Customs Declaration Service",
            CustomHeaderNames.X_CONVERSATION_ID_HEADER_NAME -> ConversationIdOne.toString
          )).get

        status(eventualResult) mustBe BAD_REQUEST
      }

      "return 400 for POST of notification when payload is invalid" in {
        val eventualResult: Future[Result] = route(app, FakeRequest(POST, "/customs-notifications-receiver-stub/pushnotifications")
          .withTextBody("SOm NOn XMl")
          .withHeaders(
            AUTHORIZATION -> CsidOne.toString,
            CONTENT_TYPE -> MimeTypes.XML,
            ACCEPT -> MimeTypes.XML,
            USER_AGENT -> "Customs Declaration Service",
            CustomHeaderNames.X_CONVERSATION_ID_HEADER_NAME -> ConversationIdOne.toString
          )).get

        status(eventualResult) mustBe BAD_REQUEST
      }

      "return 400 for GET of notifications when csid is invalid" in {
        val eventualResult: Future[Result] = route(app, FakeRequest(GET, "/customs-notifications-receiver-stub/pushnotifications/1")
          .withHeaders(
            CONTENT_TYPE -> MimeTypes.JSON
          )).get

        status(eventualResult) mustBe BAD_REQUEST
        contentAsJson(eventualResult) mustBe BadRequestJsonInvalidCsid
      }

      "return 400 for GET of count by csid when csid is invalid" in {
        val eventualResult: Future[Result] = route(app, FakeRequest(GET, "/customs-notifications-receiver-stub/count/1")
          .withHeaders(
            CONTENT_TYPE -> MimeTypes.JSON
          )).get

        status(eventualResult) mustBe BAD_REQUEST
        contentAsJson(eventualResult) mustBe BadRequestJsonInvalidCsid
      }

      "return 415 for incorrect ContentType header" in {
        val eventualResult: Future[Result] = route(app, FakeRequest(POST, "/customs-notifications-receiver-stub/pushnotifications")
          .withXmlBody(XmlPayload)
          .withHeaders(
            AUTHORIZATION -> CsidOne.toString,
            CONTENT_TYPE -> MimeTypes.TEXT,
            ACCEPT -> MimeTypes.XML,
            USER_AGENT -> "Customs Declaration Service",
            CustomHeaderNames.X_CONVERSATION_ID_HEADER_NAME -> ConversationIdOne.toString
          )).get

        status(eventualResult) mustBe UNSUPPORTED_MEDIA_TYPE
        val x = contentAsString(eventualResult)
        string2xml(x) mustBe UnsupportedMediaTypeXml
      }

    }
  }

  private def awaitValidPost(csid: CsId, conversationId: ConversationId): Result =  await(validPost(csid, conversationId))

  private def validPost(csid: CsId, conversationId: ConversationId): Future[Result] =
    route(app, FakeRequest(POST, "/customs-notifications-receiver-stub/pushnotifications")
      .withXmlBody(XmlPayload)
      .withHeaders(
        AUTHORIZATION -> csid.toString,
        CONTENT_TYPE -> MimeTypes.XML,
        USER_AGENT -> "Customs Declaration Service",
        CustomHeaderNames.X_CONVERSATION_ID_HEADER_NAME -> conversationId.toString
      )).get

}
