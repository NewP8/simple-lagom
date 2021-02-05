organization in ThisBuild := "com.example"
version in ThisBuild := sys.env.get("SERVICE_VERSION").getOrElse("1.0.0")

// per aggiornare versione a ultimo commit git https://developer.lightbend.com/guides/openshift-deployment/lagom/building-using-sbt.html
//version in ThisBuild ~= (_.replace('+', '-'))
//dynver in ThisBuild ~= (_.replace('+', '-'))

// the Scala version that will be used for cross-compiled libraries
scalaVersion in ThisBuild := "2.13.4"

lagomKafkaPropertiesFile in ThisBuild :=
  Some(
    (baseDirectory in ThisBuild).value / "project" / "kafka-server.properties"
  )

val postgresDriver = "org.postgresql" % "postgresql" % "42.2.18"
val macwire = "com.softwaremill.macwire" %% "macros" % "2.3.3" % "provided"
val scalaTest = "org.scalatest" %% "scalatest" % "3.1.1" % Test
val akkaDiscoveryKubernetesApi =
  "com.lightbend.akka.discovery" %% "akka-discovery-kubernetes-api" % "1.0.9"
// usata per scoprire nodi per formazione inizialer di cluster
// (https://developer.lightbend.com/guides/openshift-deployment/lagom/forming-a-cluster.html)

// ThisBuild / scalacOptions ++= List("-encoding", "utf8", "-deprecation", "-feature", "-unchecked", "-Xfatal-warnings")


def dockerSettings =
  Seq(
    dockerUpdateLatest := true,
    dockerBaseImage := "adoptopenjdk:11-jre-hotspot",
//    dockerUsername := sys.props.get("docker.username"),
//    dockerRepository := sys.props.get("docker.registry")
  )

lazy val `simple` = (project in file("."))
  .aggregate(`simple-api`, `simple-impl`, `sentinella-api`, `sentinella-impl`)

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
      lagomScaladslKafkaBroker,
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

lazy val `sentinella-api` = (project in file("sentinella-api"))
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslApi
    )
  )

lazy val `sentinella-impl` = (project in file("sentinella-impl"))
  .enablePlugins(LagomScala)
  .settings(
    libraryDependencies ++= Seq(
      lagomScaladslKafkaClient,
      lagomScaladslTestKit,
      macwire,
      scalaTest,
      lagomScaladslAkkaDiscovery,
      akkaDiscoveryKubernetesApi
    )
  )
  .settings(dockerSettings)
  .dependsOn(`sentinella-api`, `simple-api`)

lagomCassandraEnabled in ThisBuild := false
