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

  // Slick and Database
  "org.postgresql" % "postgresql" % "42.7.4",
  "org.playframework" %% "play-slick" % "6.2.0",
  "org.playframework" %% "play-slick-evolutions" % "6.2.0",

)

