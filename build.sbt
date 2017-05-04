enablePlugins(JavaAppPackaging)

name := "ChatCluster"

version := "1.0"

scalaVersion := "2.12.2"

val catsVersion      = "0.9.0"
val circeVersion     = "0.7.0"
val akkaVersion      = "2.4.17"
val akkaHttpVersion  = "10.0.4"
val akkaKafkaVersion = "0.14"
val akkaPersistenceCassandraVersion = "0.23"

libraryDependencies ++= Seq(
  "org.typelevel"     %% "cats"                       % catsVersion,
  "io.circe"          %% "circe-core"                 % circeVersion,
  "io.circe"          %% "circe-generic"              % circeVersion,
  "io.circe"          %% "circe-parser"               % circeVersion,
  "com.typesafe.akka" %% "akka-actor"                 % akkaVersion,
  "com.typesafe.akka" %% "akka-stream"                % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-tools"         % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding"      % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence"           % akkaVersion,
  "com.typesafe.akka" %% "akka-persistence-cassandra" % akkaPersistenceCassandraVersion,
  "com.typesafe.akka" %% "akka-http"                  % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-stream-kafka"          % akkaKafkaVersion
)

packageName in Docker := "chatservice"
version in Docker     := "latest"