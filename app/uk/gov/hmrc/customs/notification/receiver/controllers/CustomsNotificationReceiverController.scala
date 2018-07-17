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
import play.api.mvc.{Action, AnyContent, Headers, Result}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse.{apply => _, _}
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.notification.receiver.models.NotificationRequest._
import uk.gov.hmrc.customs.notification.receiver.models.{CustomHeaderNames, Header, NotificationRequest}
import uk.gov.hmrc.customs.notification.receiver.services.PersistenceService
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}


@Singleton
class CustomsNotificationReceiverController @Inject()(logger : CdsLogger, persistenceService: PersistenceService ) extends BaseController {

  def post(): Action[AnyContent] = Action.async { implicit request =>

    request.body.asXml match {
      case Some(xmlPayload) =>
        val either: Either[Result, NotificationRequest] = for {
          authHeader <- extractHeader(AUTHORIZATION, request.headers).right
          conversationId <- extractHeader(CustomHeaderNames.X_CONVERSATION_ID_HEADER_NAME, request.headers).right

        } yield {
          val seqOfHeader = request.headers.toSimpleMap.map(t => Header(t._1, t._2)).toSeq
          val payload = xmlPayload.toString
          NotificationRequest(extractCsid(authHeader), conversationId, authHeader, seqOfHeader, payload)
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

  private def extractCsid(authHeadersId: String): UUID = {
    val six = 6
    val fortyTwo = 42
    UUID.fromString(authHeadersId.substring(six, fortyTwo))
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

}
