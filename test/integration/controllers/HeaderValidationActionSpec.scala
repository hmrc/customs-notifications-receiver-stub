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

import org.scalatestplus.mockito.MockitoSugar
import play.api.mvc.{AnyContentAsEmpty, Result}
import play.api.test.Helpers.{ACCEPT, AUTHORIZATION, CONTENT_TYPE, USER_AGENT}
import play.api.test.{FakeRequest, Helpers}
import uk.gov.hmrc.customs.common.controllers.ErrorResponse.ErrorContentTypeHeaderInvalid
import uk.gov.hmrc.customs.common.logging.CdsLogger
import uk.gov.hmrc.customs.notification.receiver.controllers.HeaderValidationAction
import uk.gov.hmrc.customs.notification.receiver.models.{CustomHeaderNames, ExtractedHeadersRequest}
import util.TestData.conversationId1
import util.{TestData, UnitSpec}

class HeaderValidationActionSpec extends UnitSpec with MockitoSugar {

  val mockLogger = mock[CdsLogger]
  val headerValidationAction = new HeaderValidationAction(mockLogger, Helpers.stubControllerComponents())

  "in happy path" should {
    "return 415 when CONTENT_TYPE header is invalid" in {
      val eitherResult: Either[Result, ExtractedHeadersRequest[AnyContentAsEmpty.type]] = await(headerValidationAction.refine(FakeRequest()
        .withHeaders( AUTHORIZATION -> ("Basic " + TestData.csId1),
          CONTENT_TYPE -> "invalid",
          ACCEPT -> "application/xml",
          USER_AGENT -> "Customs Declaration Service",
          CustomHeaderNames.X_CONVERSATION_ID_HEADER_NAME -> conversationId1.toString)))

      eitherResult shouldBe Left(ErrorContentTypeHeaderInvalid.XmlResult)
    }

  }
}
