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

package uk.gov.hmrc.customs.notification.receiver.controllers

import com.google.inject.Inject
import org.bson.types.ObjectId
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents}
import uk.gov.hmrc.customs.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.common.controllers.ErrorResponse._
import uk.gov.hmrc.customs.common.logging.CdsLogger
import uk.gov.hmrc.customs.notification.receiver.models.NotificationRequest._
import uk.gov.hmrc.customs.notification.receiver.models._
import uk.gov.hmrc.customs.notification.receiver.repo.NotificationRequestRecordRepo
import uk.gov.hmrc.play.bootstrap.backend.controller.BackendController

import java.time.{LocalDateTime, ZoneOffset}
import java.util.UUID
import javax.inject.Singleton
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.language.postfixOps
import scala.util.{Failure, Success, Try}


@Singleton
class CustomsNotificationReceiverController @Inject()(logger: CdsLogger,
                                                      headerValidationAction: HeaderValidationAction,
                                                      repo: NotificationRequestRecordRepo,
                                                      cc: ControllerComponents)
                                                     (implicit ec: ExecutionContext) extends BackendController(cc) {

  def post(): Action[AnyContent] = Action andThen headerValidationAction async { implicit extractedHeadersRequest =>
    //TODO AS THIS IS MOCKING THE CLIENT WE WOULD LIKE THIS TO RETURN 500 SO MANY TIMES AND THEN RETURN OK
    logger.debug(s"extractedHeadersRequest.request == ${extractedHeadersRequest.request}")

    extractedHeadersRequest.body.asXml match {
      case Some(xmlPayload) =>
        val seqOfHeader = extractedHeadersRequest.headers.toSimpleMap.map(t => Header(t._1, t._2)).toSeq
        val payloadAsString = xmlPayload.toString
        val notificationRequest = NotificationRequest(extractedHeadersRequest.csid, extractedHeadersRequest.conversationId, extractedHeadersRequest.authHeader, seqOfHeader.toList,LocalDateTime.now(ZoneOffset.UTC), payloadAsString)
        logger.debug(s"Received Notification for: [${notificationRequest.csId}], headers=[$seqOfHeader]")
        repo.insertNotificationRequestRecord(NotificationRequestRecord(notificationRequest, LocalDateTime.now(ZoneOffset.UTC), new ObjectId()))

        val functionCode: String = Try {
          val payload = notificationRequest.xmlPayload
          val functionCodeIndex = payload.indexOf("p:FunctionCode")
          payload.subSequence(functionCodeIndex, functionCodeIndex + 20).toString
        }.getOrElse("FailedToGetFunctionCode")

        def checkPayloadStatus():scala.concurrent.Future[play.api.mvc.Result] = {
          payloadAsString match {
            case payloadAsString if payloadAsString.contains("failWith-500") => countTimesReturned()
            case payloadAsString if payloadAsString.contains("failWith-400") => Future.successful(BadRequest(Json.toJson(notificationRequest)))
            case _ => Future.successful(Ok(Json.toJson(notificationRequest)))
          }
        }

        def countTimesReturned(): scala.concurrent.Future[play.api.mvc.Result] = {
          functionCode match {
            case functionCode if functionCode.contains("01") =>
              logger.debug(s"Time: ${LocalDateTime.now()} Function Code = 01")
              countResponse("01")
            case functionCode if functionCode.contains("09") =>
              logger.debug(s"Time: ${LocalDateTime.now()} Function Code = 09")
              countResponse("09")
            case functionCode if functionCode.contains("13") =>
              logger.debug(s"Time: ${LocalDateTime.now()} Function Code = 13")
              countResponse("13")
            case _ =>
              logger.debug(s"Time: ${LocalDateTime.now()} Function Code = XX")
              countResponse("XX")
            }
          }

          def countResponse(functionCode: String): scala.concurrent.Future[play.api.mvc.Result] = {
            val countNotificationsByConversationId = Await.result(repo.countNotificationsByConversationId(notificationRequest.conversationId), 5 seconds)
            logger.debug(s"Total notifications for conversationId #${ notificationRequest.conversationId } is [${countNotificationsByConversationId}]")
            logger.debug(s"receiveNotification [$functionCode]")
            if (countNotificationsByConversationId > 10) {
              logger.debug(s"RETURN SUCCESS")
              Future.successful(Ok(Json.toJson(notificationRequest)))
            } else {
             Future.successful(InternalServerError(Json.toJson(notificationRequest)))
            }
          }

          checkPayloadStatus()

      case None =>
        val message = "Invalid Xml"
        logger.error(message)
        Future.successful(errorBadRequest(message).XmlResult)
    }
  }

  def retrieveNotificationByCsId(csid: String): Action[AnyContent] = Action.async { request =>
    logger.debug(s"Trying to get Notifications by CsId:[$csid]\nheaders=\n[${request.headers.toSimpleMap}]")

    Try(UUID.fromString(csid)) match {
      case Success(uuid) =>
        val eventuallyNotifications  = repo.findAllByCsId(CsId(uuid))
        eventuallyNotifications.map { seqNotifications =>
          logger.debug(s"""Found Notifications for Csid [$csid${seqNotifications.mkString("\n","\n","")}]""")
          Ok(Json.toJson(seqNotifications))
        }
      case Failure(e) =>
        logger.error("Bad request", e)
        Future.successful(errorBadRequest(e.getMessage).JsonResult)
    }
  }

  def retrieveNotificationByConversationId(conversationId: String): Action[AnyContent] = Action.async { request =>
    logger.debug(s"Trying to get Notifications by ConversationId:[$conversationId\nheaders=\n${request.headers.toSimpleMap}]")

    Try(UUID.fromString(conversationId)) match {
      case Success(uuid) =>
        val eventuallyNotifications = repo.findAllByConversationId(ConversationId(uuid))
        eventuallyNotifications.map { seqNotifications =>
          if (seqNotifications.isEmpty) {
            logger.debug(s"Notifications for ConversationId [$conversationId] is empty")
            ErrorNotFound.XmlResult
          } else {
            logger.debug(s""""Found Notifications for ConversationId [$conversationId${seqNotifications.mkString("\n","\n","")}]""")
            Ok(Json.toJson(seqNotifications))
          }
        }
      case Failure(e) =>
        logger.error("Bad request", e)
        Future.successful(errorBadRequest(e.getMessage).JsonResult)
    }
  }

  def countNotificationByCsId(csid: String): Action[AnyContent] = Action.async { _ =>
    Try(UUID.fromString(csid)) match {
      case Success(csidUuid) =>
        repo.countNotificationsByCsId(CsId(csidUuid)).map { count =>
          logger.debug(s"About to get counts by CsId:[$csid] count=[$count]")
          Ok(Json.parse(s"""{"count": "$count"}"""))
        }
      case Failure(e) =>
        logger.error(s"Invalid csid UUID [$csid]")
        Future.successful(errorBadRequest(e.getMessage).JsonResult)
    }
  }

  def countNotificationByConversationId(conversationId: String): Action[AnyContent] = Action.async { _ =>
    Try(UUID.fromString(conversationId)) match {
      case Success(csidUuid) =>
        repo.countNotificationsByConversationId(ConversationId(csidUuid)).map { count =>
          logger.debug(s"About to get counts by conversationId:[$conversationId] count=[$count]")
          Ok(Json.parse(s"""{"count": "$count"}"""))
        }
      case Failure(e) =>
        logger.error(s"Invalid csid UUID [$conversationId]")
        Future.successful(errorBadRequest(e.getMessage).JsonResult)
    }
  }

  def countAllNotifications: Action[AnyContent] = Action.async { _ =>
    repo.countAllNotifications().map { count =>
      logger.debug(s"All notifications count=[$count]")
      Ok(Json.parse(s"""{"count": "$count"}"""))
    }
  }

  def clearNotifications(): Action[AnyContent] = Action.async { _ =>
    logger.debug("Clearing down Notifications")
    repo.dropCollection()
    Future.successful(NoContent)
  }

  def customResponse(statusCode: Int): Action[AnyContent] = Action.async {
    logger.debug(s"Responding with HTTP status [$statusCode] as requested")

    val result = statusCode match {
      case code if (code >= 300) && (code < 400) =>
        Redirect(routes.CustomsNotificationReceiverController.post(), code)
      case _ =>
        ErrorResponse(statusCode, "REQUESTED_ERROR", s"Returning HTTP status $statusCode as requested").XmlResult
    }

    Future.successful(result)
  }
}
