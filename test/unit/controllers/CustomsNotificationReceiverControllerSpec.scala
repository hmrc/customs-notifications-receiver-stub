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

import org.mockito.Mockito._
import org.scalatest.BeforeAndAfterEach
import org.scalatest.mockito.MockitoSugar
import play.api.test.FakeRequest
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.notification.receiver.controllers.{CustomsNotificationReceiverController, HeaderValidationAction}
import uk.gov.hmrc.customs.notification.receiver.services.PersistenceService
import uk.gov.hmrc.play.test.UnitSpec

class CustomsNotificationReceiverControllerSpec extends UnitSpec with BeforeAndAfterEach with MockitoSugar{

    val mockPersistenceService = mock[PersistenceService]
    val mockHeaderValidationAction = mock[HeaderValidationAction]
    val mockLogger = mock[CdsLogger]
    lazy val testController = new CustomsNotificationReceiverController(mockLogger, mockHeaderValidationAction, mockPersistenceService)


  override def beforeEach(): Unit = {
    reset(mockPersistenceService, mockLogger)
    when(mockPersistenceService.clearAll()).thenReturn(())
  }

  "clear endpoint should call clearNotifications in Service" should {
    await(testController.clearNotifications().apply(FakeRequest()))
    verify(mockPersistenceService).clearAll()
  }
}
