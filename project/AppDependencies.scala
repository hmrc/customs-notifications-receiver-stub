import sbt._

object AppDependencies {

  private val testScope = "test,it"
  private val mongoDbVersion = "1.3.0"

  val compile = Seq(
    "uk.gov.hmrc"                    %% "customs-api-common"   % "1.60.0",
    "uk.gov.hmrc.mongo"              %% "hmrc-mongo-play-28"   % mongoDbVersion
  )

  val test = Seq(
    "org.scalatestplus.play" %% "scalatestplus-play"       % "5.1.0"          % testScope,
    "uk.gov.hmrc"            %% "customs-api-common"       % "1.60.0"         % testScope classifier "tests",
    "com.github.tomakehurst" %  "wiremock-standalone"      % "2.27.2"         % testScope,
    "org.scalatestplus"      %% "mockito-3-4"              % "3.2.10.0"       % testScope,
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-28"  % mongoDbVersion   % testScope,
    "com.vladsch.flexmark"   %  "flexmark-all"             % "0.35.10"        % testScope
  )
}
