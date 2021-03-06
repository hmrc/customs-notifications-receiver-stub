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

package unit.logging

import uk.gov.hmrc.customs.notification.receiver.logging.LoggingHelper
import util.UnitSpec
import util.TestData._

class LoggingHelperSpec extends UnitSpec {

  "LoggingHelper" should {
    "format log message" in {
      LoggingHelper.logMsgPrefix(NotificationRequestOne) shouldBe "[conversationId=eaca01f9-ec3b-4ede-b263-61b626dde232][clientSubscriptionId=ffff01f9-ec3b-4ede-b263-61b626dde232]"
    }
  }

}
