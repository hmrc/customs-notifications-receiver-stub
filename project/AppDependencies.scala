import sbt._

object AppDependencies {

  private lazy val customsApiCommonVersion = "1.57.0"
  private lazy val mongoDbVersion = "0.73.0"
  private lazy val testScope = "test,it"

  val compile = Seq(
    "uk.gov.hmrc" %% "customs-api-common" % customsApiCommonVersion,
    "uk.gov.hmrc.mongo"   %% "hmrc-mongo-play-28" % mongoDbVersion,
    "com.github.ghik" % "silencer-lib" % "1.7.5" % Provided cross CrossVersion.full,
    compilerPlugin("com.github.ghik" % "silencer-plugin" % "1.7.5" cross CrossVersion.full)
  )

  val test = Seq(
    "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % testScope,
    "uk.gov.hmrc" %% "customs-api-common" % customsApiCommonVersion % testScope classifier "tests",
    "com.github.tomakehurst" % "wiremock-standalone" % "2.27.1" % testScope,
    "org.scalatestplus" %% "mockito-3-4" % "3.2.9.0" % testScope,
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-test-play-28" % mongoDbVersion % testScope,
    "com.vladsch.flexmark" % "flexmark-all" % "0.35.10" % testScope
  )
}
