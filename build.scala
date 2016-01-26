import sbt._
import Keys._

object ScalaDataflowBuild extends Build {
  val projectName = "ScalaDataflow"

  def extraLibraryDependencies = Seq(
    libraryDependencies ++= Seq(
      // Note this library will quickly become out of date.
			"com.google.cloud.dataflow" % "google-cloud-dataflow-java-sdk-all" % "1.3.0"
    )
  )

  def scalaSettings = Seq(
    scalaVersion := "2.11.7",
    scalacOptions ++= Seq(
      "-optimize",
      "-unchecked",
      "-deprecation",
      "-feature"
    )
  )

  def updateOnDependencyChange = Seq(
    watchSources <++= (managedClasspath in Test) map { cp => cp.files })

  lazy val root = {
    val settings =
      Project.defaultSettings ++
      extraLibraryDependencies ++
      scalaSettings ++
      updateOnDependencyChange ++
      Seq(name := projectName, fork := true)

    Project(id = projectName, base = file("."), settings = settings)
  }
}
