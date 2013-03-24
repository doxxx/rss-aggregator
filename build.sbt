name := "rss-aggregator"

scalaVersion := "2.10.0"

resolvers += "Sonatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots/"

libraryDependencies ++= List(
    "org.slf4j" % "slf4j-simple" % "1.7.4",
    "com.typesafe.akka" %% "akka-actor" % "2.1.2",
    "org.mongodb" %% "casbah" % "2.5.1",
    "rome" % "rome" % "1.0"
)
