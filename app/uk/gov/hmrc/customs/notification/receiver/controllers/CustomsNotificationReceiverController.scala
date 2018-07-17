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
import javax.inject.Singleton

import com.google.inject.Inject
import play.api.libs.json.Json
import play.api.mvc.{Action, AnyContent, Headers, Result}
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.notification.receiver.models.NotificationRequest._
import uk.gov.hmrc.customs.notification.receiver.models.{CustomHeaderNames, Header, NotificationRequest}
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.collection.immutable.Seq
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.xml.NodeSeq


@Singleton
class CustomsNotificationReceiverController @Inject()(logger : CdsLogger) extends BaseController {

  private val notificationsByCsidMap = scala.collection.mutable.Map[UUID, Seq[NotificationRequest]]()

  def post(): Action[NodeSeq] = Action.async(parse.xml) { implicit request =>
    // TODO: Xml action type/parsing/error does not look right
    val body: Seq[String] = request.body.map {
      xml =>
        val s = xml.toString
        logger.info(s)
        s
    }

    val either: Either[Result, NotificationRequest] = for {
     authHeader <- extractHeader(AUTHORIZATION, request.headers).right
     conversationId <- extractHeader(CustomHeaderNames.X_CONVERSATION_ID_HEADER_NAME, request.headers).right

    } yield {
      val seqOfHeader = request.headers.toSimpleMap.map(t => Header(t._1, t._2)).toSeq
      val payload = body.head.toString
      NotificationRequest(extractCsid(authHeader), conversationId, authHeader, seqOfHeader, payload)
    }

    either match {
      case Right(notificationRequest) =>
        notificationsByCsidMap.get(notificationRequest.csid).fold[Unit](notificationsByCsidMap.put(notificationRequest.csid, Seq(notificationRequest))) {notifications: Seq[NotificationRequest] =>
          val newList = notifications :+ notificationRequest
          notificationsByCsidMap.put(notificationRequest.csid, newList)
        }
        Future.successful(Ok(Json.toJson(notificationRequest)))
      case Left(result) =>
        Future.successful(result)
    }

  }

  private def extractCsid(authHeadersId: String): UUID = {
    val six = 6
    val fortyTwo = 42
    UUID.fromString(authHeadersId.substring(six, fortyTwo))
  }

  def retrieveNotificationByCsId(csid: String): Action[AnyContent] = Action.async { request =>
    Try(UUID.fromString(csid)) match {
      case Success(csidUuid) =>
        val notifications: Seq[NotificationRequest] = notificationsByCsidMap.get(csidUuid).fold[Seq[NotificationRequest]](Seq.empty)(ns => ns)
        Future.successful(Ok(Json.toJson(notifications)))
      case Failure(e) =>
        Future.successful(BadRequest(e.getMessage))
    }
  }

  def extractHeader(key: String, headers: Headers): Either[Result, String] = {
    val maybeString: Option[String] = headers.get(key)
    maybeString.fold[Either[Result, String]](Left(BadRequest))(headerVal => Right(headerVal))
  }

}
