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

package support

import com.codahale.metrics.SharedMetricRegistries
import com.google.inject.AbstractModule
import org.scalatest.freespec.AnyFreeSpecLike
import org.scalatest.time.{Millis, Seconds, Span}
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.inject.Injector
import play.api.inject.guice.{GuiceApplicationBuilder, GuiceableModule}
import play.api.mvc.Result
import play.api.test.Helpers.{await, defaultAwaitTimeout}
import play.api.test.{DefaultTestServerFactory, RunningServer}
import play.api.{Application, Mode}
import play.core.server.ServerConfig
import uk.gov.hmrc.customs.notification.receiver.repo.NotificationRequestRecordRepo
import util.{RichMatchers, WireMockSupport}

import scala.concurrent.ExecutionContext

/**
 * This is common spec for every test case which brings all of useful routines we want to use in our scenarios.
 */

trait ItSpec
  extends AnyFreeSpecLike
    with RichMatchers
    with WireMockSupport
    with GuiceOneServerPerSuite { self =>

  val testServerPort = 19001

  implicit lazy val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  private val module: AbstractModule = new AbstractModule {
    override def configure(): Unit = ()
  }

  private val configMap: Map[String, Any] = Map[String, Any](
    "mongodb.uri " -> "mongodb://localhost:27017/customs-notifications-receiver-stub-it"
  )

  lazy val injector: Injector = fakeApplication().injector
  lazy val repository: NotificationRequestRecordRepo = injector.instanceOf[NotificationRequestRecordRepo]

  override implicit val patienceConfig: PatienceConfig = PatienceConfig(
    timeout = scaled(Span(5, Seconds)),
    interval = scaled(Span(300, Millis)))

  override def fakeApplication(): Application = new GuiceApplicationBuilder()
    .overrides(GuiceableModule.fromGuiceModules(Seq(module)))
    .configure(configMap).build()

  override def beforeEach(): Unit = {
    SharedMetricRegistries.clear()
    super.beforeEach()
    await(repository.dropCollection())
    await(repository.ensureIndexes())
  }

  def status(of: Result): Int = of.header.status

  override implicit protected lazy val runningServer: RunningServer =
    TestServerFactory.start(app)

  object TestServerFactory extends DefaultTestServerFactory {
    override protected def serverConfig(app: Application): ServerConfig = {
      val sc = ServerConfig(port    = Some(testServerPort), sslPort = None, mode = Mode.Test, rootDir = app.path)
      sc.copy(configuration = sc.configuration.withFallback(overrideServerConfiguration(app)))
    }
  }
}
