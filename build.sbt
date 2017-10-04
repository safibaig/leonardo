import Settings._
import Testing._

lazy val root = project.in(file("."))
  .settings(rootSettings:_*)
  .withTestSettings

Revolver.settings

Revolver.enableDebugging(port = 5051, suspend = false)

mainClass in reStart := Some("org.broadinstitute.dsde.workbench.leonardo.Boot")

// When JAVA_OPTS are specified in the environment, they are usually meant for the application
// itself rather than sbt, but they are not passed by default to the application, which is a forked
// process. This passes them through to the "re-start" command, which is probably what a developer
// would normally expect.
javaOptions in reStart ++= sys.env.get("JAVA_OPTS").map(_.split(" ").toSeq).getOrElse(Seq.empty)

resolvers += Resolver.sonatypeRepo("releases")

addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.4")

// if your project uses multiple Scala versions, use this for cross building
addCompilerPlugin("org.spire-math" % "kind-projector" % "0.9.4" cross CrossVersion.binary)

// if your project uses both 2.10 and polymorphic lambdas
libraryDependencies ++= (scalaBinaryVersion.value match {
  case "2.10" =>
    compilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full) :: Nil
  case _ =>
    Nil
})