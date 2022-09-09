import scalariform.formatter.preferences._

lazy val Versions = new {
  val scalaTest = "3.2.13"
  val scalaVersion = "2.13.8"
}

lazy val Libraries = new {
  val scalaTest               = "org.scalatest"     %% "scalatest"                  % Versions.scalaTest   % "test"
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
      Libraries.scalaTest
    ),
    Test / parallelExecution := false
  ))


lazy val `background-refresh-zio` = project
  .in(file("background-refresh-zio"))
  .settings(Seq(
    libraryDependencies ++= Seq(
      Libraries.scalaTest
    ),
    Test / parallelExecution := false
  ))
