name := "price-alerter"

lazy val scala331 = "3.3.1"
lazy val scala2138 = "2.13.8"
lazy val supportedScalaVersions = List(scala331, scala2138)

scalacOptions in (Compile, doc) ++= Seq(
  "-no-link-warnings" // Suppresses problems with Scaladoc @throws links
)

resolvers += Resolver.mavenCentral

// Release / Publish definitions
ThisBuild / organization         := "com.bigelectrons"
ThisBuild / scalaVersion         := scala331

lazy val root = project.in(file("."))
  .dependsOn(priceExtractor)
  //.aggregate(price-extractor, openelectrons-module2)
  .settings(
    // crossScalaVersions must be set to Nil on the aggregating project
    crossScalaVersions := Nil,
    publish / skip := true
  )

lazy val priceExtractor = (project in file("price-extractor"))
  .settings(
    name := "price-extractor",
    //crossScalaVersions := supportedScalaVersions,
    scalaVersion := scala331,
    libraryDependencies ++= Seq(
      "org.jsoup" % "jsoup" % "1.17.2",
      "org.playframework" %% "play-json" % "3.0.6",
      "com.microsoft.playwright" % "playwright" % "1.55.0",
      "org.scalatest" %% "scalatest" % "3.2.19" % Test
    )
  )
