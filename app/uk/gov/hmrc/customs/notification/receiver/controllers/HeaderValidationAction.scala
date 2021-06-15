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

package uk.gov.hmrc.customs.notification.receiver.controllers

import java.util.UUID

import javax.inject.{Inject, Singleton}
import play.api.http.HeaderNames
import play.api.mvc.{ActionRefiner, ControllerComponents, Request, Result}
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.{ErrorContentTypeHeaderInvalid, ErrorGenericBadRequest}
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.notification.receiver.models.{ConversationId, CsId, CustomHeaderNames, ExtractedHeadersRequest}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.matching.Regex

@Singleton
class HeaderValidationAction @Inject()(logger: CdsLogger, cc: ControllerComponents)(implicit ec: ExecutionContext) extends ActionRefiner[Request, ExtractedHeadersRequest] {

  override def executionContext: ExecutionContext = cc.executionContext

  private val uuidRegex = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$".r
  private val xmlRegex = s"^${MimeTypes.XML}.*".r

  override def refine[A](r: Request[A]): Future[Either[Result, ExtractedHeadersRequest[A]]] = {
    Future.successful{
      for {
        _ <- validateAndExtract(r, HeaderNames.CONTENT_TYPE, xmlRegex, ErrorContentTypeHeaderInvalid).right
        conversationId <- validateAndExtract(r, CustomHeaderNames.X_CONVERSATION_ID_HEADER_NAME, uuidRegex, ErrorGenericBadRequest).right
        authHeader <- validateAndExtract(r, HeaderNames.AUTHORIZATION, uuidRegex, ErrorGenericBadRequest).right
      } yield ExtractedHeadersRequest(CsId(UUID.fromString(authHeader)), ConversationId(UUID.fromString(conversationId)), authHeader,  r)
    }
  }

  private def validateAndExtract[A](request: Request[A], headerName: String, regex: Regex, errorResponse: ErrorResponse): Either[Result, String] = {
    val mayBeHeaderValue = request.headers.get(headerName)
    mayBeHeaderValue.fold[Either[Result, String]]{
      logger.error(s"Unable to retrieve header:$headerName from the request")
      Left(errorResponse.XmlResult)
    }{ headerValue: String =>
      val matcher = regex.pattern.matcher(headerValue)
      if (matcher.find()) {
        logger.debug(s"header:$headerName retrieved and validated, value was: $headerValue")
        Right(headerValue)
      } else {
        logger.error(s"header:$headerName was invalid value:$headerValue")
        Left(errorResponse.XmlResult)
      }
    }
  }

}
