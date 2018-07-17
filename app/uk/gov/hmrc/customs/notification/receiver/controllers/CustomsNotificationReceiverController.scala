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

import com.google.inject.Inject
import javax.inject.Singleton
import play.api.http.HeaderNames
import play.api.libs.json.Json
import play.api.mvc._
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.{apply => _, _}
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.notification.receiver.models.NotificationRequest._
import uk.gov.hmrc.customs.notification.receiver.models.{CustomHeaderNames, Header, NotificationRequest}
import uk.gov.hmrc.customs.notification.receiver.services.PersistenceService
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.util.matching.Regex
import scala.util.{Failure, Success, Try}


@Singleton
class CustomsNotificationReceiverController @Inject()(logger : CdsLogger, persistenceService: PersistenceService ) extends BaseController {

  def post(): Action[AnyContent] = Action andThen new ValidationAction async{ implicit request =>
    request.body.asXml match {
      case Some(xmlPayload) => {
        val seqOfHeader = request.headers.toSimpleMap.map(t => Header(t._1, t._2)).toSeq
        val payload = xmlPayload.toString
        val notificationRequest = NotificationRequest(request.csid, request.conversationId.toString, request.authHeader, seqOfHeader, payload)
        persistenceService.persist(notificationRequest)
        Future.successful(Ok(Json.toJson(notificationRequest)))}
      case None =>
        Future.successful(errorBadRequest("Invalid Xml").XmlResult)
    }
  }

  def retrieveNotificationByCsId(csid: String): Action[AnyContent] = Action.async { _ =>
    Try(UUID.fromString(csid)) match {
      case Success(csidUuid) =>
        val notifications: Seq[NotificationRequest] = persistenceService.notificationsById(csidUuid)
        Future.successful(Ok(Json.toJson(notifications)))
      case Failure(e) =>
        Future.successful(errorBadRequest(e.getMessage).JsonResult)
    }
  }

  def clearNotifications(): Action[AnyContent] = Action.async { _ =>
    persistenceService.clearAll()
    Future.successful(NoContent)
  }
}


class ValidationAction extends ActionRefiner[Request, ExtractedHeadersRequest] {

  private val uuidRegex = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$".r
  private val xmlRegex = s"^${MimeTypes.XML}".r
  private val csidRegex = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}".r

  override protected def refine[A](r: Request[A]): Future[Either[Result, ExtractedHeadersRequest[A]]] = {
    Future.successful{
      for {
        _ <- extractAndValidate(r, HeaderNames.ACCEPT, xmlRegex, ErrorAcceptHeaderInvalid).right
        _ <- extractAndValidate(r, HeaderNames.CONTENT_TYPE, xmlRegex, ErrorContentTypeHeaderInvalid).right
        conversationId <- extractAndValidate(r, CustomHeaderNames.X_CONVERSATION_ID_HEADER_NAME, uuidRegex, ErrorGenericBadRequest).right
        authHeader <- extractAndValidate(r, HeaderNames.AUTHORIZATION, csidRegex, ErrorGenericBadRequest).right
      } yield ExtractedHeadersRequest(extractCsid(authHeader), UUID.fromString(conversationId), authHeader,  r)
    }
  }

  private def extractCsid(authHeadersId: String): UUID = {
    val six = 6
    val fortyTwo = 42
    UUID.fromString(authHeadersId.substring(six, fortyTwo))
  }

  private def extractAndValidate[A](request: Request[A], headerName: String, regex: Regex, errorResponse: ErrorResponse): Either[Result, String] = {
    val mayBeHeaderValue = request.headers.get(headerName)
    mayBeHeaderValue.fold[Either[Result, String]]({
      Left(errorResponse.XmlResult)
    })({
      headerValue : String =>

        val matcher = regex.pattern.matcher(headerValue)
        if (!matcher.find()) {
          Left(errorResponse.XmlResult)
        } else{
          Right(headerValue)
        }
    })
  }
}

case class ExtractedHeadersRequest[A](csid: UUID, conversationId: UUID, authHeader: String, request: Request[A]) extends WrappedRequest(request)
