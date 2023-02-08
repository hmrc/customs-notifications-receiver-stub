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

package integration.controllers

import play.api.libs.json.Json
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test._
import play.mvc.Http.MimeTypes
import support.ItSpec
import uk.gov.hmrc.customs.notification.receiver.models.{ConversationId, CsId, CustomHeaderNames}
import util.TestData._

import scala.concurrent.Future

class CustomsNotificationReceiverControllerSpec extends ItSpec {

  "For CsId" - {
    "Insert some records, count and retrieve them" in {
      val insert1Result: Future[Result] = insertNotificationRequestRecord(csId1, conversationId1)
      status(await(insert1Result)) shouldBe OK
      contentType(insert1Result) shouldBe Some("application/json")
      contentAsJson(insert1Result) shouldBe notificationRequestJson(csId1, conversationId1)
      Thread.sleep(100)

      val insert2Result: Future[Result] = insertNotificationRequestRecord(csId2, conversationId2)
      status(await(insert2Result)) shouldBe OK
      contentType(insert2Result) shouldBe Some("application/json")
      contentAsJson(insert2Result) shouldBe notificationRequestJson(csId2, conversationId2)
      Thread.sleep(100)

      val insert3Result: Future[Result] = insertNotificationRequestRecord(csId1, conversationId2)
      status(await(insert3Result)) shouldBe OK
      contentType(insert3Result) shouldBe Some("application/json")
      contentAsJson(insert3Result) shouldBe notificationRequestJson(csId1, conversationId2)
      Thread.sleep(100)

      val countAllResult = route(app, FakeRequest(GET, "/customs-notifications-receiver-stub/pushnotifications/totalcount")).get
      status(await(countAllResult)) shouldBe OK
      contentType(countAllResult) shouldBe Some("application/json")
      contentAsJson(countAllResult) shouldBe Json.parse("{\"count\": \"3\"}")
      Thread.sleep(100)

      val countByCsIdResult = route(app, FakeRequest(GET, "/customs-notifications-receiver-stub/pushnotifications/count/csid/" + csId1)).get
      status(await(countByCsIdResult)) shouldBe OK
      contentType(countByCsIdResult) shouldBe Some("application/json")
      contentAsJson(countByCsIdResult) shouldBe Json.parse("{\"count\": \"2\"}")
      Thread.sleep(100)

      val csIdSearch1Result: Future[Result] = route(app, FakeRequest(GET, "/customs-notifications-receiver-stub/pushnotifications/csid/" + csId1)).get
      status(await(csIdSearch1Result)) shouldBe OK
      contentType(csIdSearch1Result) shouldBe Some("application/json")
      contentAsJson(csIdSearch1Result) shouldBe notificationsResultJson(
        notificationRequest(csId1, conversationId1),
        notificationRequest(csId1, conversationId2))
      Thread.sleep(100)

      val csIdSearch2Result: Future[Result] = route(app, FakeRequest(GET, "/customs-notifications-receiver-stub/pushnotifications/csid/" + csId2)).get
      status(await(csIdSearch2Result)) shouldBe OK
      contentType(csIdSearch2Result) shouldBe Some("application/json")
      contentAsJson(csIdSearch2Result) shouldBe notificationsResultJson(
        notificationRequest(csId2, conversationId2))
      Thread.sleep(100)
    }
  }

  "For ConversationId" - {
    //TODO debug why this being removed breaks the below test
    "return empty list for a client subscription Id that has no notifications" in {
      val result: Future[Result] = route(app, FakeRequest(GET, "/customs-notifications-receiver-stub/pushnotifications/csid/" + csId1).withHeaders(AUTHORIZATION -> ("Basic " + csId1))).get

      status(await(result)) shouldBe OK
      contentType(result) shouldBe Some("application/json")
      contentAsJson(result) shouldBe Json.parse("[]")
    }

    "Insert some records, count and retrieve them" in {
      val insert1Result: Future[Result] = insertNotificationRequestRecord(csId1, conversationId1)
      status(await(insert1Result)) shouldBe OK
      contentType(insert1Result) shouldBe Some("application/json")
      contentAsJson(insert1Result) shouldBe notificationRequestJson(csId1, conversationId1)
      Thread.sleep(100)

      val insert2Result: Future[Result] = insertNotificationRequestRecord(csId2, conversationId2)
      status(await(insert2Result)) shouldBe OK
      contentType(insert2Result) shouldBe Some("application/json")
      contentAsJson(insert2Result) shouldBe notificationRequestJson(csId2, conversationId2)
      Thread.sleep(100)

      val insert3Result: Future[Result] = insertNotificationRequestRecord(csId2, conversationId1)
      status(await(insert3Result)) shouldBe OK
      contentType(insert3Result) shouldBe Some("application/json")
      contentAsJson(insert3Result) shouldBe notificationRequestJson(csId2, conversationId1)
      Thread.sleep(100)

      val countAllResult = route(app, FakeRequest(GET, "/customs-notifications-receiver-stub/pushnotifications/totalcount")).get
      status(await(countAllResult)) shouldBe OK
      contentType(countAllResult) shouldBe Some("application/json")
      contentAsJson(countAllResult) shouldBe Json.parse("{\"count\": \"3\"}")
      Thread.sleep(100)

      val countByConversationIdResult = route(app, FakeRequest(GET, "/customs-notifications-receiver-stub/pushnotifications/count/conversationid/" + conversationId1)).get
      status(await(countByConversationIdResult)) shouldBe OK
      contentType(countByConversationIdResult) shouldBe Some("application/json")
      contentAsJson(countByConversationIdResult) shouldBe Json.parse("{\"count\": \"2\"}")
      Thread.sleep(100)

      val conversationIdSearch1Result: Future[Result] = route(app, FakeRequest(GET, "/customs-notifications-receiver-stub/pushnotifications/conversationid/" + conversationId1)).get
      status(await(conversationIdSearch1Result)) shouldBe OK
      contentType(conversationIdSearch1Result) shouldBe Some("application/json")
      contentAsJson(conversationIdSearch1Result) shouldBe notificationsResultJson(
        notificationRequest(csId1, conversationId1),
        notificationRequest(csId2, conversationId1))
      Thread.sleep(100)

      val conversationIdSearch2Result: Future[Result] = route(app, FakeRequest(GET, "/customs-notifications-receiver-stub/pushnotifications/conversationid/" + conversationId2)).get
      status(await(conversationIdSearch2Result)) shouldBe OK
      contentType(conversationIdSearch2Result) shouldBe Some("application/json")
      contentAsJson(conversationIdSearch2Result) shouldBe notificationsResultJson(
        notificationRequest(csId2, conversationId2))
      Thread.sleep(100)
    }
  }

  "For other functionality" - {
    "return empty list for a client subscription Id that has no notifications" in {
      val result: Future[Result] = route(app, FakeRequest(GET, "/customs-notifications-receiver-stub/pushnotifications/csid/" + csId1).withHeaders(AUTHORIZATION -> ("Basic " + csId1))).get

      status(await(result)) shouldBe OK
      contentType(result) shouldBe Some("application/json")
      contentAsJson(result) shouldBe Json.parse("[]")
    }

    "return NoContent when DELETE sent to pushNotifications endpoint" in {
      val result = route(app, FakeRequest(DELETE, "/customs-notifications-receiver-stub/pushnotifications").withXmlBody(XmlPayload)
        .withHeaders(
          AUTHORIZATION -> csId1.toString,
          CONTENT_TYPE -> MimeTypes.XML,
          ACCEPT -> MimeTypes.XML,
          USER_AGENT -> "Customs Declaration Service"
        )).get

      status(await(result)) shouldBe NO_CONTENT
    }
  }

  "For unhappy path" - {
    "return 400 for POST of notification when Authorisation header is missing" in {
      val result: Future[Result] = route(app, FakeRequest(POST, "/customs-notifications-receiver-stub/pushnotifications")
        .withXmlBody(XmlPayload)
        .withHeaders(
          CONTENT_TYPE -> MimeTypes.XML,
          ACCEPT -> MimeTypes.XML,
          USER_AGENT -> "Customs Declaration Service",
          CustomHeaderNames.X_CONVERSATION_ID_HEADER_NAME -> csId1.toString
        )).get

      status(await(result)) shouldBe BAD_REQUEST
    }

    "return 400 for POST of notification when payload is invalid" in {
      val result: Future[Result] = route(app, FakeRequest(POST, "/customs-notifications-receiver-stub/pushnotifications")
        .withTextBody("SOm NOn XMl")
        .withHeaders(
          AUTHORIZATION -> csId1.toString,
          CONTENT_TYPE -> MimeTypes.XML,
          ACCEPT -> MimeTypes.XML,
          USER_AGENT -> "Customs Declaration Service",
          CustomHeaderNames.X_CONVERSATION_ID_HEADER_NAME -> csId1.toString
        )).get

      status(await(result)) shouldBe BAD_REQUEST
    }

    "return 400 for GET of notifications when csid is invalid" in {
      val result: Future[Result] = route(app, FakeRequest(GET, "/customs-notifications-receiver-stub/pushnotifications/csid/1")
        .withHeaders(
          CONTENT_TYPE -> MimeTypes.JSON
        )).get

      status(await(result)) shouldBe BAD_REQUEST
      contentAsJson(result) shouldBe BadRequestJsonInvalidCsid
    }

    "return 400 for GET of count by csid when csid is invalid" in {
      val result: Future[Result] = route(app, FakeRequest(GET, "/customs-notifications-receiver-stub/pushnotifications/count/csid/1")
        .withHeaders(
          CONTENT_TYPE -> MimeTypes.JSON
        )).get

      status(await(result)) shouldBe BAD_REQUEST
      contentAsJson(result) shouldBe BadRequestJsonInvalidCsid
    }

    "return 415 for incorrect ContentType header" in {
      val result: Future[Result] = route(app, FakeRequest(POST, "/customs-notifications-receiver-stub/pushnotifications")
        .withXmlBody(XmlPayload)
        .withHeaders(
          AUTHORIZATION -> csId1.toString,
          CONTENT_TYPE -> MimeTypes.TEXT,
          ACCEPT -> MimeTypes.XML,
          USER_AGENT -> "Customs Declaration Service",
          CustomHeaderNames.X_CONVERSATION_ID_HEADER_NAME -> csId1.toString
        )).get

      status(await(result)) shouldBe UNSUPPORTED_MEDIA_TYPE
      string2xml(contentAsString(result)) shouldBe UnsupportedMediaTypeXml
    }
  }

  private def insertNotificationRequestRecord(csid: CsId, conversationId: ConversationId): Future[Result] = {
    route(app,
      FakeRequest(POST, "/customs-notifications-receiver-stub/pushnotifications")
        .withXmlBody(XmlPayload)
        .withHeaders(
          AUTHORIZATION -> csid.toString,
          CONTENT_TYPE -> MimeTypes.XML,
          USER_AGENT -> "Customs Declaration Service",
          CustomHeaderNames.X_CONVERSATION_ID_HEADER_NAME -> conversationId.toString
        )).get
  }
}
