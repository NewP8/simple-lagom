organization in ThisBuild := "com.example"
version in ThisBuild := "1.0-SNAPSHOT"

// the Scala version that will be used for cross-compiled libraries
scalaVersion in ThisBuild := "2.13.0"

val postgresDriver = "org.postgresql" % "postgresql" % "42.2.18"

val macwire = "com.softwaremill.macwire" %% "macros" % "2.3.3" % "provided"
val scalaTest = "org.scalatest" %% "scalatest" % "3.1.1" % Test

// val akkaDiscoveryKubernetesApi = "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api"                 % "1.0.9"
// val lagomScaladslAkkaDiscovery = "com.lightbend.lagom"          %% "lagom-scaladsl-akka-discovery-service-locator" % lagomVersion
// vedere per deploy (o minikube)

// ThisBuild / scalacOptions ++= List("-encoding", "utf8", "-deprecation", "-feature", "-unchecked", "-Xfatal-warnings")

// ---------------------------
// |* configurazione DOCKER *|
// ---------------------------

//def dockerSettings = Seq(
//  dockerUpdateLatest := true,
//  dockerBaseImage := getDockerBaseImage(),
//  dockerUsername := sys.props.get("docker.username"),
//  dockerRepository := sys.props.get("docker.registry")
//)
//
//def getDockerBaseImage(): String = sys.props.get("java.version") match {
//  case Some(v) if v.startsWith("11") => "adoptopenjdk/openjdk11"
//  case _                             => "adoptopenjdk/openjdk8"
//}

lazy val `simple` = (project in file("."))
  .aggregate(`simple-api`, `simple-impl`)

lazy val `simple-api` = (project in file("simple-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )

lazy val `simple-impl` = (project in file("simple-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslPersistenceCassandra, // write side
      lagomScaladslPersistenceJdbc, // read side
      // lagomScaladslKafkaBroker,
      lagomScaladslTestKit,
      macwire,
      scalaTest,
      postgresDriver,
//      lagomScaladslAkkaDiscovery,
//      akkaDiscoveryKubernetesApi,
      "com.typesafe.akka" %% "akka-persistence-testkit" % "2.6.8" % Test
    )
  )
  //.settings(dockerSettings)
  .settings(lagomForkedTestSettings)
  .dependsOn(`simple-api`)

lagomKafkaEnabled in ThisBuild := false
