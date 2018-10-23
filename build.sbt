import sbtassembly.MergeStrategy

name := "akka-http-oauth2-server"

version := "1.0.0"

scalaVersion := "2.12.7"

test in assembly := {}

mainClass in (Compile, run) := Some("ws.gmax.OAuth2ServiceBoot")

val akkaVersion = "2.5.17"
val akkaHttpVersion = "10.1.5"
val scalaTestVersion = "3.0.5"
val mockitoVersion = "1.10.19"
val loggingVersion = "3.9.0"
val jwtVersion = "0.18.0"
val slickVersion = "3.2.3"
val c3p0Version = "0.9.1.2"
val h2DbVersion = "1.4.197"
val commonsVersion = "3.8.1"
val jodaVersion  = "2.10"
val guavaVersion = "26.0-jre"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-core" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.scala-logging" %% "scala-logging" % loggingVersion,
  "com.pauldijou" %% "jwt-core" % jwtVersion,
  "joda-time" % "joda-time" % jodaVersion,
  "com.google.guava" % "guava" % guavaVersion,
  "com.typesafe.slick" %% "slick" % slickVersion,
  "c3p0" % "c3p0" % c3p0Version,
  "com.h2database" % "h2" % h2DbVersion,
  "org.apache.commons" % "commons-lang3" % commonsVersion,

  "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-stream-testkit" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
  "org.mockito" % "mockito-all" % mockitoVersion % Test
)

enablePlugins(DockerPlugin, JavaAppPackaging)

assemblyMergeStrategy in assembly := {
  case PathList("META-INF", "io.netty.versions.properties", xs@_*) => MergeStrategy.last
  case PathList("META-INF", "io.netty.versions.properties") => MergeStrategy.last
  case PathList("io", "netty", xs@_*) => MergeStrategy.last
  case "about.html" => MergeStrategy.rename
  case "logback.xml" => MergeStrategy.first
  case "application.conf" => MergeStrategy.first
  case "META-INF/io.netty.versions.properties" => MergeStrategy.first
  case PathList("org", "apache", xs@_*) => MergeStrategy.first
  case PathList("org", "slf4j", xs@_*) => MergeStrategy.first
  case PathList("akka", "stream", xs@_*) => MergeStrategy.first
  case x => (assemblyMergeStrategy in assembly).value(x)
}

scalacOptions := Seq(
  "-deprecation", // warning and location for usages of deprecated APIs
  "-feature", // warning and location for usages of features that should be imported explicitly
  "-language:implicitConversions", // Support for implicit conversions
  "-unchecked", // additional warnings where generated code depends on assumptions
  "-Xcheckinit", // runtime error when a val is not initialized due to trait hierarchies (instead of NPE somewhere else)
  "-Ywarn-inaccessible",
  "-Ywarn-dead-code"
)

scalacOptions in (Compile, doc) ++= Seq("-unchecked", "-deprecation", "-diagrams", "-implicits", "-skip-packages", "samples")