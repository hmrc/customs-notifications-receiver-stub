import play.sbt.PlayImport.PlayKeys.playDefaultPort
import sbt.Keys._
import sbt.Tests.{Group, SubProcess}
import sbt._
import uk.gov.hmrc.DefaultBuildSettings.addTestReportOption
import uk.gov.hmrc.gitstamp.GitStampPlugin._

import scala.language.postfixOps

name := "customs-notifications-receiver-stub"

lazy val CdsIntegrationComponentTest = config("it") extend Test

val testConfig = Seq(CdsIntegrationComponentTest, Test)

def forkedJvmPerTestConfig(tests: Seq[TestDefinition], packages: String*): Seq[Group] =
  tests.groupBy(_.name.takeWhile(_ != '.')).filter(packageAndTests => packages contains packageAndTests._1) map {
    case (packg, theTests) =>
      Group(packg, theTests, SubProcess(ForkOptions()))
  } toSeq

lazy val testAll = TaskKey[Unit]("test-all")
lazy val allTest = Seq(testAll := (CdsIntegrationComponentTest / test).dependsOn(Test / test).value)

lazy val microservice = (project in file("."))
  .enablePlugins(PlayScala)
  .enablePlugins(SbtDistributablesPlugin)
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)
  .configs(testConfig: _*)
  .settings(
    scalaVersion := "2.13.16",
    commonSettings,
    unitTestSettings,
    integrationComponentTestSettings,
    allTest,
    scoverageSettings
  )
  .settings(majorVersion := 0)
  .settings(scalacOptions += "-Wconf:cat=unused-imports&src=target/scala-2\\.13/routes/.*:s")
  .settings(playDefaultPort := 9826)

lazy val unitTestSettings =
  inConfig(Test)(Defaults.testTasks) ++
    Seq(
      Test / testOptions := Seq(Tests.Filter(unitTestFilter)),
      Test / unmanagedSourceDirectories := Seq((Test / baseDirectory).value / "test"),
      addTestReportOption(Test, "test-reports")
    )

lazy val integrationComponentTestSettings =
  inConfig(CdsIntegrationComponentTest)(Defaults.testTasks) ++
    Seq(
      CdsIntegrationComponentTest / testOptions := Seq(Tests.Filter(integrationComponentTestFilter)),
      CdsIntegrationComponentTest /  parallelExecution := false,
      addTestReportOption(CdsIntegrationComponentTest, "int-comp-test-reports"),
      CdsIntegrationComponentTest / testGrouping := forkedJvmPerTestConfig((Test / definedTests).value, "integration", "component")
    )

lazy val commonSettings: Seq[Setting[_]] = gitStampSettings

lazy val scoverageSettings: Seq[Setting[_]] = Seq(
  coverageExcludedPackages := List(
    "<empty>"
    ,".*Reverse.*"
    ,".*(AuthService|BuildInfo|Routes).*"
  ).mkString(";"),
  coverageMinimumStmtTotal := 92,
  coverageFailOnMinimum := true,
  coverageHighlighting := true,
  Test / parallelExecution := false
)

def integrationComponentTestFilter(name: String): Boolean = (name startsWith "integration") || (name startsWith "component")
def unitTestFilter(name: String): Boolean = name startsWith "unit"

scalastyleConfig := baseDirectory.value / "project" / "scalastyle-config.xml"

Compile / unmanagedResourceDirectories += baseDirectory.value / "public"

libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test
