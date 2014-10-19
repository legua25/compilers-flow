name := "flow-compiler"

version := "0.1"

scalaVersion := "2.11.2"

libraryDependencies ++= Seq(
  "com.tunnelvisionlabs" % "antlr4-master" % "4.4"
)

antlr4Settings

EclipseKeys.withSource := true
