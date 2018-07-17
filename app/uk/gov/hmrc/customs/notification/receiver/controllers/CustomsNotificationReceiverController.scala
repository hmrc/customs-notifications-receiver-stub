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
import play.api.libs.json.Json
import play.api.mvc._
import play.mvc.Http.MimeTypes
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.{apply => _, _}
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.notification.receiver.controllers.ValidationAction.extractCsid
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
      case Some(xmlPayload) =>
        val either: Either[Result, NotificationRequest] = for {
          authHeader <- extractHeader(AUTHORIZATION, request.headers).right
        } yield {
          val seqOfHeader = request.headers.toSimpleMap.map(t => Header(t._1, t._2)).toSeq
          val payload = xmlPayload.toString
          NotificationRequest(request.csid, request.conversationId.toString, authHeader, seqOfHeader, payload)
        }

        either match {
          case Right(notificationRequest) =>
            persistenceService.persist(notificationRequest)
            Future.successful(Ok(Json.toJson(notificationRequest)))
          case Left(result) =>
            Future.successful(result)
        }
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

  def extractHeader(key: String, headers: Headers): Either[Result, String] = {
    val maybeString: Option[String] = headers.get(key)
    maybeString.fold[Either[Result, String]](Left(ErrorGenericBadRequest.XmlResult))(headerVal => Right(headerVal))
  }

  def extractAndValidateContentTypeHeader(headers: Headers): Either[Result, String] = {
    val maybeString: Option[String] = headers.get(CONTENT_TYPE)
    maybeString.fold[Either[Result, String]]({
      Left(ErrorGenericBadRequest.XmlResult)
    })(headerVal =>
      if (headerVal.contains(MimeTypes.XML)) {
        Right(headerVal)
      } else {
        Left(ErrorGenericBadRequest.XmlResult)
      }
    )
  }

}


class ValidationAction extends ActionRefiner[Request, ExtractedHeadersRequest] {
  /*
  auth
  conv
  acc
  cont
   */

  private val uuidRegex = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$".r
  private val xmlRegex = s"^${MimeTypes.XML}".r
  private val csidRegex = "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}".r

  override protected def refine[A](r: Request[A]): Future[Either[Result, ExtractedHeadersRequest[A]]] = {
    Future.successful{
      for {
       //_ <- extractAndValidate(r, "Accept", xmlRegex, ErrorAcceptHeaderInvalid).right
        //_ <- extractAndValidate(r, "Content-Type", ".*".r, ErrorContentTypeHeaderInvalid).right
        conversationId <- extractAndValidate(r, CustomHeaderNames.X_CONVERSATION_ID_HEADER_NAME, uuidRegex, ErrorGenericBadRequest).right
        csid <- extractAndValidate(r, "AUTHORIZATION", csidRegex, ErrorContentTypeHeaderInvalid).right
      } yield ExtractedHeadersRequest(UUID.fromString(conversationId), extractCsid(csid), r)
    }
  }

  private def extractAndValidate[A](request: Request[A], headerName: String, regex: Regex, errorResponse: ErrorResponse): Either[Result, String] = {
    val mayBeHeaderValue = request.headers.get(headerName)
    mayBeHeaderValue.fold[Either[Result, String]]({
      Left(errorResponse.XmlResult)
    })({
      headerValue : String =>
        val matcher = regex.pattern.matcher(headerValue)
        if (!matcher.matches()) {
          Left(errorResponse.XmlResult)
        } else{
          Right(headerValue)
        }
    })
  }
}

object ValidationAction {
  def extractCsid(authHeadersId: String): UUID = {
    val six = 6
    val fortyTwo = 42
    UUID.fromString(authHeadersId.substring(six, fortyTwo))
  }
}

case class ExtractedHeadersRequest[A](csid: UUID, conversationId: UUID, request: Request[A]) extends WrappedRequest(request)