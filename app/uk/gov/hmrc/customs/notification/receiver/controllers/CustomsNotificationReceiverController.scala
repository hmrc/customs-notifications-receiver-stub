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
import play.api.mvc.{Action, AnyContent, Headers}
import uk.gov.hmrc.customs.api.common.logging.CdsLogger
import uk.gov.hmrc.customs.notification.receiver.models.{CustomHeaderNames, NotificationRequest}
import uk.gov.hmrc.customs.notification.receiver.models.NotificationRequest._
import uk.gov.hmrc.play.bootstrap.controller.BaseController

import scala.concurrent.Future
import scala.xml.NodeSeq


@Singleton
class CustomsNotificationReceiverController @Inject()(logger : CdsLogger) extends BaseController {

  def extractCsid(authHeadersid: String): UUID = {
    UUID.fromString(authHeadersid.substring(6,42))
  }

  private val requestMap: Map[String, NotificationRequest] = Map.empty

  def post(): Action[NodeSeq] = Action.async(parse.xml) { req =>
    val body = req.body.map {
      xml =>
        logger.info(xml.toString())
        xml.toString()
    }

    val results: Either[Status, NotificationRequest] = for {
     authHeader <- extractHeader(AUTHORIZATION, req.headers).right
     conversationId <- extractHeader(CustomHeaderNames.X_CONVERSATION_ID_HEADER_NAME, req.headers).right

    } yield NotificationRequest(extractCsid(authHeader), conversationId, authHeader, Seq.empty, body.toString())

    results match {
      case Right(notificationRequest) => { //TODO add request to map
        Future.successful(Ok(Json.toJson(notificationRequest)))
      }
      case Left(result) => Future.successful(result)
    }

  }

  def retrieveNotificationByCsId(csid: String): Action[AnyContent] = Action { req =>
    val clientSubscriptionId = req.headers.get(AUTHORIZATION).fold()(headerVal => extractCsid(headerVal))
    Ok("ok " + clientSubscriptionId)
  }

  def extractHeader(key: String, headers: Headers): Either[Status, String] = {
    val maybeString: Option[String] = headers.get(key)
    maybeString.fold[Either[Status, String]](Left(BadRequest))(headerVal => Right(headerVal))
  }

}
