name := "PunORama"

version := "0.0.1"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "commons-io" % "commons-io" % "2.4",
  "org.scalactic" %% "scalactic" % "2.2.6",
  "org.scalatest" %% "scalatest" % "2.2.6" % "test",
  "com.google.cloud.dataflow" % "google-cloud-dataflow-java-sdk-all" % "1.3.0"
)