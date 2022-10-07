import scalariform.formatter.preferences._

lazy val Versions = new {
  val akka = "2.6.20"
  val zio = "2.0.2"
  val zioLogging = "2.1.0"
  val logbackClassic = "1.2.8"
  val logstashLogbackEncoder = "7.1"
  val scalaTest = "3.2.13"
  val scalaVersion = "2.13.8"
}

lazy val Libraries = new {
  val scalaTest               = "org.scalatest"     %% "scalatest"                  % Versions.scalaTest   % "test"

  val zioProjectLibraries = Seq(
    "dev.zio" %% "zio" % Versions.zio,
    "dev.zio" %% "zio-streams" % Versions.zio,
    "dev.zio" %% "zio-json" % "0.3.0-RC11",
    "dev.zio" %% "zio-actors" % "0.1.0",
    "dev.zio" %% "zio-logging" % Versions.zioLogging,
    "dev.zio" %% "zio-logging-slf4j" % Versions.zioLogging,
    "ch.qos.logback" % "logback-classic" % Versions.logbackClassic,
    "net.logstash.logback" % "logstash-logback-encoder" % Versions.logstashLogbackEncoder,
    scalaTest,
    "dev.zio" %% "zio-test" % Versions.zio % "test",
    "dev.zio" %% "zio-test-sbt" % Versions.zio % "test",
    "dev.zio" %% "zio-test-magnolia" % Versions.zio % "test"
  )
}

ThisBuild / scalaVersion := Versions.scalaVersion

ThisBuild / organization := "au.id.fsat"
ThisBuild / organizationName := "Felix Satyaputra"
ThisBuild / startYear := Some(2022)

scalariformPreferences := scalariformPreferences.value
    .setPreference(AlignSingleLineCaseStatements, true)
    .setPreference(AllowParamGroupsOnNewlines, true)

lazy val `scala-concurrency-comparison` = project
  .in(file("."))
  .aggregate(
    `background-refresh-akka-typed`,
    `background-refresh-zio`
  )
  .settings(
    Test / parallelExecution := false
  )

lazy val `background-refresh-akka-typed` = project
  .in(file("background-refresh-akka-typed"))
  .settings(Seq(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-actor" % Versions.akka,
      "com.typesafe.akka" %% "akka-actor-typed" % Versions.akka,
      "com.typesafe.akka" %% "akka-stream" % Versions.akka,
      "ch.qos.logback" % "logback-classic" % Versions.logbackClassic,
      Libraries.scalaTest
    ),
    Test / parallelExecution := false
  ))


lazy val `background-refresh-zio` = project
  .in(file("background-refresh-zio"))
  .settings(Seq(
    libraryDependencies ++= Libraries.zioProjectLibraries,
    Test / parallelExecution := false,
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  ))

lazy val `fsm-zio` = project
  .in(file("fsm-zio"))
  .settings(Seq(
    libraryDependencies ++= Libraries.zioProjectLibraries,
    Test / parallelExecution := false,
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  ))

lazy val `actors-zio` = project
  .in(file("actors-zio"))
  .settings(Seq(
    libraryDependencies ++= Libraries.zioProjectLibraries,
    Test / parallelExecution := false,
    testFrameworks += new TestFramework("zio.test.sbt.ZTestFramework")
  ))
