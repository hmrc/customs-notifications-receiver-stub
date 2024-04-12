import sbt._

object AppDependencies {

  private val hmrcMongoVersion = "1.8.0"
  private val playSuffix       = "-play-30"
  private val bootstrapVersion = "8.5.0"

  val compile = Seq(
  "uk.gov.hmrc"         %% s"bootstrap-backend$playSuffix" % bootstrapVersion,
  "uk.gov.hmrc.mongo"   %% s"hmrc-mongo$playSuffix"        % hmrcMongoVersion
  )

  val test = Seq(
    "org.scalatestplus" %% "mockito-3-4"                   % "3.2.10.0",
    "uk.gov.hmrc.mongo" %% s"hmrc-mongo-test$playSuffix"   % hmrcMongoVersion,
    "uk.gov.hmrc"       %% s"bootstrap-test$playSuffix"    % bootstrapVersion
  ).map(_ % Test)
}
