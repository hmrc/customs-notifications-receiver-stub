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
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, ControllerComponents, Result}
import uk.gov.hmrc.customs.common.controllers.ErrorResponse
import uk.gov.hmrc.customs.common.controllers.ErrorResponse._
import uk.gov.hmrc.customs.common.logging.CdsLogger
import uk.gov.hmrc.customs.notification.receiver.Utils
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

  /** This is the traders endpoint that the notification will be sent to. </br></br>
   * We return Future.successful(Ok) to represent a 200 response (trader received the notification),
   * or Future.successful(InternalServerError) for 400 (e.g. we sent incorrect XML to them),
   * or Future.successful(BadRequest) for a 500 (server error) */
  def post(): Action[AnyContent] = Action andThen headerValidationAction async { implicit extractedHeadersRequest =>
    // Thread.sleep(15000) // TODO: remove before commit, this is just to slow things down for testing
    logger.debug(s"extractedHeadersRequest.request == ${extractedHeadersRequest.request}")

    extractedHeadersRequest.body.asXml match {
      case None =>
        val message = "Invalid Xml"
        logger.error(message)
        Future.successful(errorBadRequest(message).XmlResult)
      case Some(xmlPayload) =>
        val seqOfHeader = extractedHeadersRequest.headers.toSimpleMap.map(t => Header(t._1, t._2)).toSeq
        val payloadAsString = xmlPayload.toString
        val notificationRequest = NotificationRequest(extractedHeadersRequest.csid,
          extractedHeadersRequest.conversationId,
          extractedHeadersRequest.authHeader,
          seqOfHeader.toList,
          LocalDateTime.now(ZoneOffset.UTC),
          payloadAsString)

        // save a record of this notification being attempted to be processed, so can test against this
        repo.insertNotificationRequestRecord(NotificationRequestRecord(notificationRequest))

        payloadAsString match {
          case s if s.contains("failWith-500") =>
            val numberOf5xxToReturn = s
              .split("_retry")
              .lift(1)
              .flatMap(_.takeWhile(_.isDigit) match {
                case ""  => None
                case num => Some(num.toInt)
              })

            handle5xx(numberOf5xxToReturn.getOrElse(1), notificationRequest)

          case s if s.contains("failWith-400") =>
            logger.warn("*Bad Request, returning 400 response*")
            Future.successful(BadRequest(Json.toJson("""{"message":"custom 4xx handler response from trader"}""")))

          case _ =>
            logger.info("*Success, returning 200 response*")
            Future.successful(Ok(Json.toJson(
              s"""{"message":"well done, this is a 200 response from the trader's system regarding ${notificationRequest.conversationId}"}"""
            )))
        }
    }
  }

  /**
   * timesUntilSuccess: e.g. 1 = finally return a 200 if there has already been 1 (timesUntilSuccess) 5xx response for this notification */
  def handle5xx(timesUntilSuccess: Int, notificationRequest: NotificationRequest): Future[Result] = {
    val countNotificationsByContents = Await.result(repo.countByHash(Utils.hashNotificationContents(notificationRequest.xmlPayload)), 5 seconds)

    logger.debug(s"Total notifications for conversationId #${ notificationRequest.conversationId } is [${countNotificationsByContents}]")

    if (countNotificationsByContents > timesUntilSuccess) {
      logger.info(s"*Client has successfully received notification! Returning OK response*")
      Future.successful(Ok(Json.toJson("")))
    } else {
      logger.warn(s"*Returning 500 response*")
      Future.successful(InternalServerError(Json.toJson("")))
    }
  }

  def retrieveNotificationByCsId(csid: String): Action[AnyContent] = Action.async { request =>
    Try(UUID.fromString(csid)) match {
      case Success(uuid) =>
        val eventuallyNotifications  = repo.findAllByCsId(CsId(uuid))
        eventuallyNotifications.map { seqNotifications =>
          logger.debug(s"Found ${ seqNotifications.size } Notifications for csId=[$csid]")
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
        logger.error("Bad request", e) // TODO: shouldn't get stack trace for a bad request
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

  def countNotificationsByContents(hash: String): Action[AnyContent] = Action.async { _ =>
    Try(hash) match {
      case Success(hash) =>
        repo.countByHash(hash).map { count =>
          Ok(Json.parse(s"""{"count": "$count"}"""))
        }
      case Failure(e) =>
        logger.error(s"Invalid hash")
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

  private def getFunctionCode(xmlPayload: String): String = {
    Try {
      val functionCodeIndex = xmlPayload.indexOf("p:FunctionCode")
      xmlPayload.subSequence(functionCodeIndex, functionCodeIndex + 20).toString // TODO: improve this, magic number
    }.getOrElse("FailedToGetFunctionCode")
  }
}
