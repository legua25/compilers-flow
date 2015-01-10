name := "flow-compiler"

version := "0.1"

scalaVersion := "2.11.5"

sbtVersion := "0.13.7"

scalacOptions ++= Seq("-feature", "-deprecation", "-language:implicitConversions")

libraryDependencies ++= Seq(
  "com.tunnelvisionlabs" %  "antlr4-master" % "4.4",
  "com.github.scopt"     %% "scopt"         % "3.3.0"
)

antlr4Settings

antlr4GenVisitor in Antlr4 := true

antlr4PackageName in Antlr4 := Some("flow.syntax")

EclipseKeys.withSource := true

EclipseKeys.createSrc := EclipseCreateSrc.Default + EclipseCreateSrc.Managed
