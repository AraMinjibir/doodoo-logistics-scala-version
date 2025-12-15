name := """doodoo-logistics"""
organization := "cyapex"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.13.16"

libraryDependencies ++= Seq(
  guice,
  evolutions,

  // Test
  "org.scalatestplus.play" %% "scalatestplus-play" % "7.0.2" % Test,

  // Slick core
  "com.typesafe.slick" %% "slick" % "3.6.0",
  "com.typesafe.slick" %% "slick-hikaricp" % "3.6.0",

  // Test DB
  "com.h2database" % "h2" % "2.2.224" % Test,

  // Play Slick
  "org.playframework" %% "play-slick" % "6.2.0",
  "org.playframework" %% "play-slick-evolutions" % "6.2.0"
)


