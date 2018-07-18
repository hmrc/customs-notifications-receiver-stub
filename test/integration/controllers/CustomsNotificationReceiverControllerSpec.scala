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

import org.scalatestplus.play._
import org.scalatestplus.play.guice.GuiceOneAppPerTest
import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test._
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.customs.notification.receiver.models.{ConversationId, CsId, CustomHeaderNames}
import util.TestData._

import scala.concurrent.Future

class CustomsNotificationReceiverControllerSpec extends PlaySpec with GuiceOneAppPerTest {

  "CustomsNotificationReceiverController" can {
    "In happy path" should {

      "return 200 response with a payload representing the inserted notification for a valid Post" in {
        val eventualResult: Future[Result] = validPost(CsidOne, ConversationIdOne)

        status(eventualResult) mustBe OK
        contentType(eventualResult) mustBe Some("application/json")
        contentAsJson(eventualResult) mustBe notificationRequestJson(CsidOne, ConversationIdOne)
      }

      "return empty list for a client subscription Id that has no notifications" in {
        val eventualResult: Future[Result] = route(app, FakeRequest(GET, "/pushnotifications/" + CsidOne).withHeaders(AUTHORIZATION -> ("Basic " + CsidOne))).get

        status(eventualResult) mustBe OK
        contentType(eventualResult) mustBe Some("application/json")
        contentAsJson(eventualResult) mustBe Json.parse("[]")
      }

      "return all received requests for a client subscription Id" in {
        awaitValidPost(CsidOne, ConversationIdOne)
        awaitValidPost(CsidOne, ConversationIdOne)
        awaitValidPost(CsidTwo, ConversationIdTwo)

        val eventualResult: Future[Result] = route(app, FakeRequest(GET, "/pushnotifications/" + CsidOne)).get

        status(eventualResult) mustBe OK
        contentType(eventualResult) mustBe Some("application/json")
        contentAsJson(eventualResult) mustBe notificationsResultJson(
          notificationRequest(CsidOne, ConversationIdOne),
          notificationRequest(CsidOne, ConversationIdOne)
        )

        val eventualResult2: Future[Result] = route(app, FakeRequest(GET, "/pushnotifications/" + CsidTwo)).get

        status(eventualResult2) mustBe OK
        contentType(eventualResult2) mustBe Some("application/json")
        contentAsJson(eventualResult2) mustBe notificationsResultJson(
          notificationRequest(CsidTwo, ConversationIdTwo)
        )
      }

      "return NoContent when DELETE sent to pushNotifications endpoint" in {
        val eventualResult = route(app, FakeRequest(DELETE, s"/pushnotifications").withXmlBody(XmlPayload)
          .withHeaders(
            AUTHORIZATION -> ("Basic " + CsidOne),
            CONTENT_TYPE -> MimeTypes.XML,
            ACCEPT -> MimeTypes.XML,
            USER_AGENT -> "Customs Declaration Service"
          )).get

        status(eventualResult) mustBe NO_CONTENT
      }

      "return empty received requests for a client subscription Id that had notifications but clear called" in {
          awaitValidPost(CsidOne, ConversationIdOne)
          awaitValidPost(CsidOne, ConversationIdOne)

        val eventualResult: Future[Result] = route(app, FakeRequest(GET, "/pushnotifications/" + CsidOne)).get

        status(eventualResult) mustBe OK
        contentType(eventualResult) mustBe Some("application/json")
        contentAsJson(eventualResult) mustBe notificationsResultJson(
          notificationRequest(CsidOne, ConversationIdOne),
          notificationRequest(CsidOne, ConversationIdOne))

        val eventualResult2 = route(app, FakeRequest(DELETE, s"/pushnotifications").withXmlBody(XmlPayload)
          .withHeaders(
            AUTHORIZATION -> ("Basic " + CsidOne),
            CONTENT_TYPE -> MimeTypes.XML,
            ACCEPT -> MimeTypes.XML,
            USER_AGENT -> "Customs Declaration Service"
          )).get

        status(eventualResult2) mustBe NO_CONTENT

        val eventualResult3: Future[Result] = route(app, FakeRequest(GET, "/pushnotifications/" + CsidOne).withHeaders(AUTHORIZATION -> ("Basic " + CsidOne))).get

        status(eventualResult3) mustBe OK
        contentType(eventualResult3) mustBe Some("application/json")
        contentAsJson(eventualResult3) mustBe Json.parse("[]")
      }
    }

    "In un happy path" should {
      "return 400 for POST of notification when Authorisation header is missing" in {
        val eventualResult: Future[Result] = route(app, FakeRequest(POST, "/pushnotifications")
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
        val eventualResult: Future[Result] = route(app, FakeRequest(POST, "/pushnotifications")
          .withTextBody("SOm NOn XMl")
          .withHeaders(
            AUTHORIZATION -> ("Basic " + CsidOne),
            CONTENT_TYPE -> MimeTypes.XML,
            ACCEPT -> MimeTypes.XML,
            USER_AGENT -> "Customs Declaration Service",
            CustomHeaderNames.X_CONVERSATION_ID_HEADER_NAME -> ConversationIdOne.toString
          )).get

        status(eventualResult) mustBe BAD_REQUEST
      }

      "return 400 for GET of notifications when csid is invalid" in {
        val eventualResult: Future[Result] = route(app, FakeRequest(GET, "/pushnotifications/1")
          .withHeaders(
            CONTENT_TYPE -> MimeTypes.JSON
          )).get

        status(eventualResult) mustBe BAD_REQUEST
        contentAsJson(eventualResult) mustBe badRequestJsonInvalidCsid
      }

      "return 415 for incorrect ContentType header" in {
        val eventualResult: Future[Result] = route(app, FakeRequest(POST, "/pushnotifications")
          .withXmlBody(XmlPayload)
          .withHeaders(
            AUTHORIZATION -> ("Basic " + CsidOne.toString),
            CONTENT_TYPE -> MimeTypes.TEXT,
            ACCEPT -> MimeTypes.XML,
            USER_AGENT -> "Customs Declaration Service",
            CustomHeaderNames.X_CONVERSATION_ID_HEADER_NAME -> ConversationIdOne.toString
          )).get

        status(eventualResult) mustBe UNSUPPORTED_MEDIA_TYPE
        val x = contentAsString(eventualResult)
        string2xml(x) mustBe unsupportedMediaTypeXml
      }

      "return 406 for incorrect Accept header" in {
        val eventualResult: Future[Result] = route(app, FakeRequest(POST, "/pushnotifications")
          .withTextBody("<SOMEXML></SOMEXML>")
          .withHeaders(
            AUTHORIZATION -> ("Basic " + CsidOne.toString),
            CONTENT_TYPE -> MimeTypes.XML,
            ACCEPT -> MimeTypes.TEXT,
            USER_AGENT -> "Customs Declaration Service",
            CustomHeaderNames.X_CONVERSATION_ID_HEADER_NAME -> ConversationIdOne.toString
          )).get

        status(eventualResult) mustBe NOT_ACCEPTABLE
        val x = contentAsString(eventualResult)
        string2xml(x) mustBe acceptHeaderInvalidXml
      }
    }
  }

  private def awaitValidPost(csid: CsId, conversationId: ConversationId): Result =  await(validPost(csid, conversationId))

  private def validPost(csid: CsId, conversationId: ConversationId): Future[Result] =
    route(app, FakeRequest(POST, s"/pushnotifications")
      .withXmlBody(XmlPayload)
      .withHeaders(
        AUTHORIZATION -> ("Basic " + csid),
        CONTENT_TYPE -> MimeTypes.XML,
        ACCEPT -> MimeTypes.XML,
        USER_AGENT -> "Customs Declaration Service",
        CustomHeaderNames.X_CONVERSATION_ID_HEADER_NAME -> conversationId.toString
      )).get

}
