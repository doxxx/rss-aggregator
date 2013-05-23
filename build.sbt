name := "rss-aggregator"

version := "1.0-SNAPSHOT"

scalaVersion := "2.10.1"

scalacOptions ++= Seq("-feature")

resolvers ++= Seq(
    "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
    "spray repo" at "http://repo.spray.io"
)

libraryDependencies ++= Seq(
    "ch.qos.logback" % "logback-classic" % "1.0.10",
    "com.typesafe.akka" %% "akka-actor" % "2.1.4",
    "com.typesafe.akka" %% "akka-slf4j" % "2.1.4",
    "org.mongodb" %% "casbah" % "2.5.1",
    "rome" % "rome" % "1.0",
    "io.spray" % "spray-caching" % "1.1-20130521",
    "io.spray" % "spray-can" % "1.1-20130521",
    "io.spray" % "spray-client" % "1.1-20130521",
    "io.spray" % "spray-routing" % "1.1-20130521",
    "io.spray" %% "spray-json" % "1.2.3",
    "com.novus" %% "salat" % "1.9.2-SNAPSHOT"
)
