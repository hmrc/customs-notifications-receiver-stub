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

package unit.controllers

import org.scalatestplus.play._
import play.api.mvc.Result
import play.api.test.Helpers._
import play.api.test._
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.customs.notification.receiver.models.CustomHeaderNames
import util.TestData._

import scala.concurrent.Future
import scala.xml.NodeSeq

class CustomsNotificationReceiverControllerSpec extends PlaySpec with OneAppPerTest {


  "CustomsNotificationReceiverController" should {

    "handle valid Post and respond appropriately" in {
      val xmlBody : NodeSeq = <stuff>
                                <moreXml>Stuff</moreXml>
                              </stuff>
      val home: Future[Result] = route(app, FakeRequest(POST, "/pushnotifications")
        .withXmlBody(xmlBody)
        .withHeaders(
          AUTHORIZATION -> ("Basic " + validClientSubscriptionId),
          CONTENT_TYPE -> MimeTypes.XML,
          USER_AGENT -> "Customs Declaration Service",
          CustomHeaderNames.X_CONVERSATION_ID_HEADER_NAME -> validConversationId
        )).get

      status(home) mustBe OK
      contentType(home) mustBe Some("application/json")
      contentAsString(home) mustBe "{\"csid\":\"ffff01f9-ec3b-4ede-b263-61b626dde232\",\"conversationId\":\"eaca01f9-ec3b-4ede-b263-61b626dde232\",\"authHeaderToken\":\"Basic ffff01f9-ec3b-4ede-b263-61b626dde232\",\"outboundCallHeaders\":[],\"xmlPayload\":\"List(<stuff>\\n                                <moreXml>Stuff</moreXml>\\n                              </stuff>)\"}"
    }

    "return all received requests for a client subscription Id" in {
      val xmlBody : NodeSeq = <stuff>
        <moreXml>Stuff</moreXml>
      </stuff>

      await(route(app, FakeRequest(POST, "/pushnotifications").withXmlBody(xmlBody)
        .withHeaders(
          AUTHORIZATION -> ("Basic " + validClientSubscriptionId),
          CONTENT_TYPE -> MimeTypes.XML,
          USER_AGENT -> "Customs Declaration Service",
          CustomHeaderNames.X_CONVERSATION_ID_HEADER_NAME -> validConversationId
          )).get)

      await(route(app, FakeRequest(POST, "/pushnotifications").withXmlBody(xmlBody)
        .withHeaders(
        AUTHORIZATION -> ("Basic " + validClientSubscriptionId),
        CONTENT_TYPE -> MimeTypes.XML,
        USER_AGENT -> "Customs Declaration Service",
        CustomHeaderNames.X_CONVERSATION_ID_HEADER_NAME -> validConversationId
      )).get)

      val home: Future[Result] = route(app, FakeRequest(GET, "/pushnotifications/" + validClientSubscriptionId)).get

      status(home) mustBe OK
      contentType(home) mustBe Some("application/json")
      contentAsString(home) must include("ok")
    }

  }

}
