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

package unit.models

import play.api.libs.json.{JsString, Json}
import play.api.libs.json.Json.toJson
import support.ItSpec
import uk.gov.hmrc.customs.notification.receiver.models.{ConversationId, CsId, NotificationRequest}
import util.TestData._
import util.UnitSpec

class NotificationModelsSpec extends UnitSpec {

  "For CsId" should {
    "when reading" in {
      val csIdJson = JsString("ffff01f9-ec3b-4ede-b263-61b626dde232")
      csIdJson.as[CsId] shouldBe csId1
    }
    "when writing" in {
      toJson(csId1) shouldBe JsString("ffff01f9-ec3b-4ede-b263-61b626dde232")
    }
  }

  "For ConversationId" should {
    "when reading" in {
      val conversationIdJson = JsString("eaca01f9-ec3b-4ede-b263-61b626dde232")
      conversationIdJson.as[ConversationId] shouldBe conversationId1
    }
    "when writing" in {
      toJson(conversationId1) shouldBe JsString("eaca01f9-ec3b-4ede-b263-61b626dde232")
    }
  }

  "For NotificationRequest" should {
    "when reading" in {
      val notificationRequestJson = Json.parse("""{"csid":"ffff01f9-ec3b-4ede-b263-61b626dde232","conversationId":"eaca01f9-ec3b-4ede-b263-61b626dde232","authHeaderToken":"testAuthHeaderToken","outboundCallHeaders":[{"name":"testHeader1","value":"value1"},{"name":"testHeader2","value":"value2"}],"xmlPayload":"testXmlPayload"}""")
      notificationRequestJson.as[NotificationRequest] shouldBe notificationRequest1
    }

    "when writing" in {
      toJson(notificationRequest1) shouldBe Json.parse("""{"csid":"ffff01f9-ec3b-4ede-b263-61b626dde232","conversationId":"eaca01f9-ec3b-4ede-b263-61b626dde232","authHeaderToken":"testAuthHeaderToken","outboundCallHeaders":[{"name":"testHeader1","value":"value1"},{"name":"testHeader2","value":"value2"}],"xmlPayload":"testXmlPayload"}""")
    }
  }

}
