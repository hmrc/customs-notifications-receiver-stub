/*
 * Copyright 2021 HM Revenue & Customs
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

import akka.util.Timeout
import org.mockito.Mockito._
import org.scalatestplus.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, Matchers}
import play.api.http.Status
import play.api.test.Helpers.{AUTHORIZATION, CONTENT_TYPE, USER_AGENT}
import play.api.test.{FakeRequest, Helpers}
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.notification.receiver.controllers.{CustomsNotificationReceiverController, HeaderValidationAction}
import uk.gov.hmrc.customs.notification.receiver.models.CustomHeaderNames
import uk.gov.hmrc.customs.notification.receiver.repo.NotificationRepo
import util.UnitSpec
import util.TestData._

import scala.concurrent.duration._
import scala.language.postfixOps

class CustomsNotificationReceiverControllerSpec extends UnitSpec with BeforeAndAfterEach with MockitoSugar with Matchers {
  implicit val timeout = Timeout(5 seconds)

  private implicit val ec = Helpers.stubControllerComponents().executionContext

  trait Setup {
    val mockPersistenceService: NotificationRepo = mock[NotificationRepo]
    val mockHeaderValidationAction: HeaderValidationAction = mock[HeaderValidationAction]
    val mockLogger: CdsLogger = mock[CdsLogger]
    lazy val testController: CustomsNotificationReceiverController =
      new CustomsNotificationReceiverController(mockLogger, new HeaderValidationAction(mockLogger, Helpers.stubControllerComponents()), mockPersistenceService, Helpers.stubControllerComponents())
  }

  "CustomsNotificationReceiverController" should {
    val fakeRequestWithHeaders = FakeRequest().withHeaders(
      AUTHORIZATION -> CsidOne.toString,
      CONTENT_TYPE -> MimeTypes.XML,
      USER_AGENT -> "Customs Declaration Service",
      CustomHeaderNames.X_CONVERSATION_ID_HEADER_NAME -> ConversationIdOne.toString
    )

    "clear endpoint should call clearNotifications in Service" in new Setup {
      await(testController.clearNotifications().apply(FakeRequest()))

      verify(mockPersistenceService).clearAll()
    }

    "return 400 when request body is not XML" in new Setup {
      private val result = testController.post().apply(fakeRequestWithHeaders)

      Helpers.status(result) shouldBe Status.BAD_REQUEST
      string2xml(Helpers.contentAsString(result)) shouldBe <errorResponse><code>BAD_REQUEST</code><message>Invalid Xml</message></errorResponse>
    }

    "return custom HTTP error status code as specified in the URL path parameter" in new Setup {
      withClue("test 403") {
        val result = testController.customResponse(403).apply(fakeRequestWithHeaders)
        Helpers.status(result) shouldBe Status.FORBIDDEN
      }

      withClue("test 504") {
        val result = testController.customResponse(504).apply(fakeRequestWithHeaders)
        Helpers.status(result) shouldBe Status.GATEWAY_TIMEOUT
      }
    }

    "return custom redirect result as specified in the URL path parameter" in new Setup {
      val redirectLocation = "/customs-notifications-receiver-stub/pushnotifications"

      withClue("test 301") {
        val result = testController.customResponse(301).apply(fakeRequestWithHeaders)
        Helpers.status(result) shouldBe Status.MOVED_PERMANENTLY
        Helpers.redirectLocation(result) shouldBe Some(redirectLocation)
      }

      withClue("test 307") {
        val result = testController.customResponse(307).apply(fakeRequestWithHeaders)
        Helpers.status(result) shouldBe Status.TEMPORARY_REDIRECT
        Helpers.redirectLocation(result) shouldBe Some(redirectLocation)
      }
    }
  }
}
