organization in ThisBuild := "com.example"
version in ThisBuild := sys.env.get("SERVICE_VERSION").getOrElse("1.0.0")

scalaVersion in ThisBuild := "2.13.4"

val postgresDriver = "org.postgresql" % "postgresql" % "42.2.18"
val macwire = "com.softwaremill.macwire" %% "macros" % "2.3.3" % "provided"
val scalaTest = "org.scalatest" %% "scalatest" % "3.1.1" % Test
val akkaDiscoveryKubernetesApi =
  "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % "1.0.9"

def dockerSettings =
  Seq(
    dockerUpdateLatest := true,
    dockerBaseImage := "adoptopenjdk:11-jre-hotspot"
  )

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
      lagomScaladslPersistenceJdbc,
      lagomScaladslTestKit,
      macwire,
      scalaTest,
      postgresDriver,
      lagomScaladslAkkaDiscovery,
      akkaDiscoveryKubernetesApi,
      "com.typesafe.akka" %% "akka-persistence-testkit" % "2.6.8" % Test
    )
  )
  .settings(dockerSettings)
  // .settings(lagomForkedTestSettings) in teooria serve solo se si usa cassandra
  .dependsOn(`simple-api`)

lagomCassandraEnabled in ThisBuild := false
lagomKafkaEnabled in ThisBuild := false
