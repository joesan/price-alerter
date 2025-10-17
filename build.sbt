name := "price-alerter"

lazy val scala313 = "3.1.3"
lazy val scala2138 = "2.13.8"
lazy val supportedScalaVersions = List(scala313, scala2138)

scalacOptions in (Compile, doc) ++= Seq(
  "-no-link-warnings" // Suppresses problems with Scaladoc @throws links
)

// Release / Publish definitions
ThisBuild / organization         := "com.openelectrons"
ThisBuild / scalaVersion         := "3.1.3"

lazy val root = project.in(file("."))
  .dependsOn(price-extractor, openelectrons-module2)
  .aggregate(price-extractor, openelectrons-module2)
  .settings(
    // crossScalaVersions must be set to Nil on the aggregating project
    crossScalaVersions := Nil,
    publish / skip := true
  )

lazy val price-extractor = (project in file("price-extractor"))

lazy val openelectrons-module2 = (project in file("openelectrons-module2"))

