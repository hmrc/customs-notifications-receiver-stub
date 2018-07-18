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

package uk.gov.hmrc.customs.notification.receiver.controllers

import java.util.UUID

import play.api.http.HeaderNames
import play.api.mvc.{ActionRefiner, Request, Result}
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.{ErrorAcceptHeaderInvalid, ErrorContentTypeHeaderInvalid, ErrorGenericBadRequest}
import uk.gov.hmrc.customs.notification.receiver.models.{CsId, CustomHeaderNames, ExtractedHeadersRequest}

import scala.concurrent.Future
import scala.util.matching.Regex

class HeaderValidationAction extends ActionRefiner[Request, ExtractedHeadersRequest] {

  private val uuidRegex = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$".r
  private val xmlRegex = s"^${MimeTypes.XML}.*".r
  private val csidRegex = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}".r

  override protected def refine[A](r: Request[A]): Future[Either[Result, ExtractedHeadersRequest[A]]] = {
    Future.successful{
      for {
        _ <- validateAndExtract(r, HeaderNames.ACCEPT, xmlRegex, ErrorAcceptHeaderInvalid).right
        _ <- validateAndExtract(r, HeaderNames.CONTENT_TYPE, xmlRegex, ErrorContentTypeHeaderInvalid).right
        conversationId <- validateAndExtract(r, CustomHeaderNames.X_CONVERSATION_ID_HEADER_NAME, uuidRegex, ErrorGenericBadRequest).right
        authHeader <- validateAndExtract(r, HeaderNames.AUTHORIZATION, csidRegex, ErrorGenericBadRequest).right
      } yield ExtractedHeadersRequest(extractCsid(authHeader), UUID.fromString(conversationId), authHeader,  r)
    }
  }

  private def validateAndExtract[A](request: Request[A], headerName: String, regex: Regex, errorResponse: ErrorResponse): Either[Result, String] = {
    val mayBeHeaderValue = request.headers.get(headerName)
    mayBeHeaderValue.fold[Either[Result, String]]{
      Left(errorResponse.XmlResult)
    }{ headerValue: String =>
      val matcher = regex.pattern.matcher(headerValue)
      if (matcher.find()) {
        Right(headerValue)
      } else{
        Left(errorResponse.XmlResult)
      }
    }
  }

  private def extractCsid(authHeadersId: String): CsId = {
    val six = 6
    val fortyTwo = 42
    UUID.fromString(authHeadersId.substring(six, fortyTwo))
  }

}
