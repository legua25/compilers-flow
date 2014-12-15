name := "flow-compiler"

version := "0.1"

scalaVersion := "2.11.4"

scalacOptions ++= Seq("-feature", "-language:implicitConversions")

libraryDependencies ++= Seq(
  "com.tunnelvisionlabs" % "antlr4-master" % "4.4"
)

antlr4Settings

antlr4GenVisitor in Antlr4 := true

antlr4PackageName in Antlr4 := Some("flow")

EclipseKeys.withSource := true

EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Managed
