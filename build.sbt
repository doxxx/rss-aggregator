name := "rss-aggregator"

version := "1.0-SNAPSHOT"

scalaVersion := "2.10.1"

scalacOptions ++= Seq("-feature")

resolvers ++= Seq(
    "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/",
    "Akka Snapshots" at "http://repo.akka.io/snapshots/",
    "spray repo" at "http://repo.spray.io/",
    "spray nightlies repo" at "http://nightlies.spray.io/"
)

libraryDependencies ++= Seq(
    "ch.qos.logback" % "logback-classic" % "1.0.10",
    "com.typesafe.akka" %% "akka-actor" % "2.2.0-RC1",
    "com.typesafe.akka" %% "akka-slf4j" % "2.2.0-RC1",
    "org.mongodb" %% "casbah" % "2.5.1",
    "rome" % "rome" % "1.0",
    "io.spray" % "spray-caching" % "1.2-20130612",
    "io.spray" % "spray-can" % "1.2-20130612",
    "io.spray" % "spray-client" % "1.2-20130612",
    "io.spray" % "spray-routing" % "1.2-20130612",
    "io.spray" %% "spray-json" % "1.2.4",
    "com.novus" %% "salat" % "1.9.2-SNAPSHOT"
)
