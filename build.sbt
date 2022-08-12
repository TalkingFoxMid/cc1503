import sbt.Keys.libraryDependencies

ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"
val commonSettings = Seq(
  libraryDependencies += "com.softwaremill.sttp.tapir" %% "tapir-core" % "1.0.1",
  libraryDependencies += "io.circe" %% "circe-core" % "0.15.0-M1",
  libraryDependencies += "com.softwaremill.sttp.tapir" %% "tapir-json-circe" % "1.0.1",
  libraryDependencies += "io.circe" %% "circe-generic" % "0.15.0-M1",
  libraryDependencies += "com.softwaremill.sttp.client3" %% "fs2" % "3.6.2",
  libraryDependencies += "io.d11" %% "zhttp" % "2.0.0-RC9",
  libraryDependencies += "com.softwaremill.sttp.tapir" %% "tapir-http4s-server" % "1.0.1",
  libraryDependencies += "org.http4s" %% "http4s-blaze-server" % "0.23.12",
  libraryDependencies += "com.softwaremill.sttp.client3" %% "core" % "3.7.0",
  libraryDependencies += "com.softwaremill.sttp.client3" %% "async-http-client-backend-cats" % "3.7.0",
  libraryDependencies += "com.softwaremill.sttp.client3" %% "async-http-client-backend-fs2" % "3.7.0",
  libraryDependencies += "dev.profunktor" %% "redis4cats-core" % "1.2.0",
  libraryDependencies += "dev.profunktor" %% "redis4cats-log4cats" % "1.2.0",
  // https://mvnrepository.com/artifact/dev.profunktor/redis4cats-effects
  libraryDependencies += "dev.profunktor" %% "redis4cats-effects" % "1.2.0",

    libraryDependencies ++= Seq(
    "org.typelevel" %% "log4cats-core"    % "2.4.0",
    "org.typelevel" %% "log4cats-slf4j"   % "2.4.0"
  ),
  libraryDependencies += "org.slf4j" % "slf4j-simple" % "1.7.36",
  addCompilerPlugin("com.olegpy" %% "better-monadic-for" % "0.3.1"),
  libraryDependencies += "org.scalatest" %% "scalatest-flatspec" % "3.2.9" % Test,
  libraryDependencies += "org.scalatest" %% "scalatest-matchers-core" % "3.2.9" % Test,
  libraryDependencies += "org.scalatest" %% "scalatest-shouldmatchers" % "3.2.9" % Test






)
lazy val root = (project in file("."))
  .settings(
    name := "cc1503"
  )
  .dependsOn(client, server)
  .aggregate(client, server)

lazy val server = (project in file("server"))
  .settings(
    name:= "cc1503"
  ).settings(commonSettings)
  .dependsOn(messaging_protocols)
  .aggregate(messaging_protocols)

lazy val messaging_protocols = (project in file("messaging_protocols"))
  .settings(
    name:= "cc1503"
  ).settings(commonSettings)

lazy val client = (project in file("client"))
  .settings(
    name:= "cc1503"
  ).settings(commonSettings)
  .dependsOn(messaging_protocols)
  .aggregate(messaging_protocols)