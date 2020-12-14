/*
 * Copyright 2020 HM Revenue & Customs
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

import javax.inject.Singleton
import com.google.inject.Inject
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.api.common.controllers.ErrorResponse._
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.notification.receiver.models.NotificationRequest._
import uk.gov.hmrc.customs.notification.receiver.models.{ConversationId, CsId, Header, NotificationRequest}
import uk.gov.hmrc.customs.notification.receiver.repo.NotificationRepo
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}


@Singleton
class CustomsNotificationReceiverController @Inject()(logger : CdsLogger,
                                                      headerValidationAction: HeaderValidationAction,
                                                      persistenceService: NotificationRepo,
                                                      cc: ControllerComponents)
                                                     (implicit ec: ExecutionContext) extends BackendController(cc) {

  def post(): Action[AnyContent] = Action andThen headerValidationAction async { implicit extractedHeadersRequest =>
    extractedHeadersRequest.body.asXml match {
      case Some(xmlPayload) =>
        val seqOfHeader = extractedHeadersRequest.headers.toSimpleMap.map(t => Header(t._1, t._2)).toSeq
        val payloadAsString = xmlPayload.toString
        val notificationRequest = NotificationRequest(extractedHeadersRequest.csid, extractedHeadersRequest.conversationId, extractedHeadersRequest.authHeader, seqOfHeader, payloadAsString)
        logger.debug(s"Received Notification for :${notificationRequest.csid}\nheaders=\n$seqOfHeader\npayload=\n$payloadAsString")
        persistenceService.persist(notificationRequest)
        Future.successful(Ok(Json.toJson(notificationRequest)))
      case None =>
        logger.error("Invalid Xml")
        Future.successful(errorBadRequest("Invalid Xml").XmlResult)
    }
  }

  def retrieveNotificationByCsId(csid: String): Action[AnyContent] = Action.async { request =>

    logger.debug(s"Trying to get Notifications by CsId:$csid\nheaders=\n${request.headers.toSimpleMap}")
    Try(UUID.fromString(csid)) match {
      case Success(uuid) =>
        val eventuallyNotifications: Future[Seq[NotificationRequest]] = persistenceService.notificationsByCsId(CsId(uuid))
        eventuallyNotifications.map{seqNotifications =>
          logger.debug(s"Found Notifications for Csid $csid\n$seqNotifications")
          Ok(Json.toJson(seqNotifications))
        }
      case Failure(e) =>
        logger.error("Bad request", e)
        Future.successful(errorBadRequest(e.getMessage).JsonResult)
    }
  }

  def retrieveNotificationByConversationId(conversationId: String): Action[AnyContent] = Action.async { request =>

    logger.debug(s"Trying to get Notifications by ConversationId:$conversationId\nheaders=\n${request.headers.toSimpleMap}")
    Try(UUID.fromString(conversationId)) match {
      case Success(uuid) =>
        val eventuallyNotifications: Future[Seq[NotificationRequest]] = persistenceService.notificationsByConversationId(ConversationId(uuid))
        eventuallyNotifications.map{seqNotifications =>
          logger.debug(s"Found Notifications for ConversationId $conversationId\n$seqNotifications")
          Ok(Json.toJson(seqNotifications))
        }
      case Failure(e) =>
        logger.error("Bad request", e)
        Future.successful(errorBadRequest(e.getMessage).JsonResult)
    }
  }

  def countNotificationByCsId(csid: String): Action[AnyContent] = Action.async { _ =>
    Try(UUID.fromString(csid)) match {
      case Success(csidUuid) =>
        persistenceService.notificationCountByCsId(CsId(csidUuid)).map{count =>
          logger.debug(s"About to get counts by CsId:$csid count=$count")
          Ok(Json.parse(s"""{"count": "$count"}"""))
        }
      case Failure(e) =>
        logger.error(s"Invalid csid UUID $csid")
        Future.successful(errorBadRequest(e.getMessage).JsonResult)
    }
  }

  def countNotificationByConversationId(conversationId: String): Action[AnyContent] = Action.async { _ =>
    Try(UUID.fromString(conversationId)) match {
      case Success(csidUuid) =>
        persistenceService.notificationCountByConversationId(ConversationId(csidUuid)).map{count =>
          logger.debug(s"About to get counts by conversationId:$conversationId count=$count")
          Ok(Json.parse(s"""{"count": "$count"}"""))
        }
      case Failure(e) =>
        logger.error(s"Invalid csid UUID $conversationId")
        Future.successful(errorBadRequest(e.getMessage).JsonResult)
    }
  }

  def countAllNotifications: Action[AnyContent] = Action.async { _ =>
    persistenceService.notificationCount.map{ count =>
      logger.debug(s"About to get count of all notifications")
      Ok(Json.parse(s"""{"count": "$count"}"""))
    }
  }

  def clearNotifications(): Action[AnyContent] = Action.async { _ =>
    logger.debug("Clearing down Notifications")
    persistenceService.clearAll()
    Future.successful(NoContent)
  }

  def customResponse(statusCode: Int): Action[AnyContent] = Action.async {
    logger.debug(s"Responding with HTTP status $statusCode as requested")

    val result = statusCode match {
      case code if (code >= 300) && (code < 400) =>
        Redirect(routes.CustomsNotificationReceiverController.post(), code)
      case _ =>
        ErrorResponse(statusCode, "REQUESTED_ERROR", s"Returning HTTP status $statusCode as requested").XmlResult
    }

    Future.successful(result)
  }
}
