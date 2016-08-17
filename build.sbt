enablePlugins(GatlingPlugin)

scalaVersion := "2.11.8"

scalacOptions := Seq(
  "-encoding", "UTF-8", "-target:jvm-1.8", "-deprecation",
  "-feature", "-unchecked", "-language:implicitConversions", "-language:postfixOps")

resolvers += "justwrote" at "http://repo.justwrote.it/releases/"

libraryDependencies += "io.gatling.highcharts" % "gatling-charts-highcharts" % "2.2.2" % "test"
libraryDependencies += "io.gatling"            % "gatling-test-framework"    % "2.2.2" % "test"
libraryDependencies += "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.4"
libraryDependencies += "com.typesafe" % "config" % "1.3.0"
libraryDependencies += "org.json4s" % "json4s-jackson_2.11" % "3.2.11"
libraryDependencies += "it.justwrote" %% "scala-faker" % "0.3"
libraryDependencies += "net.databinder.dispatch" %% "dispatch-core" % "0.11.2"