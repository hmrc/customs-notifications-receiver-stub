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

package unit.repo

import org.mongodb.scala.bson.conversions.Bson
import support.ItSpec
import util.TestData.{conversationId1, csId1}

class NotificationRequestRecordSpec extends ItSpec {
  "When building a CsId Filter" in {
    val result: Bson = repository.buildCsIdFilter(csId1)
    result.toString shouldBe "Filter{fieldName='notification.csid', value=ffff01f9-ec3b-4ede-b263-61b626dde232}"
  }
  "When building a ConversationId Filter" in {
    val result: Bson = repository.buildConversationIdFilter(conversationId1)
    result.toString shouldBe "Filter{fieldName='notification.conversationId', value=eaca01f9-ec3b-4ede-b263-61b626dde232}"
  }
}
