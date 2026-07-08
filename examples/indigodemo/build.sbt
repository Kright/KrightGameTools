import scala.sys.process._
import scala.language.postfixOps

import sbtwelcome._
import indigoplugin._

Global / onChangedBuildSource := ReloadOnSourceChanges

Test / scalaJSLinkerConfig ~= { _.withModuleKind(ModuleKind.CommonJSModule) }

lazy val gameOptions: IndigoOptions =
  IndigoOptions.defaults
    .withTitle("indigo-demo")
    .withWindowSize(1024, 768)
    .withBackgroundColor("black")
    .withAssetDirectory("assets")
    .excludeAssets {
      case p if p.endsWith(os.RelPath.rel / ".gitkeep") => true
      case _                                            => false
    }

lazy val indigodemo =
  (project in file("indigodemo"))
    .enablePlugins(ScalaJSPlugin, SbtIndigo)
    .dependsOn(
      ProjectRef(file("../../"), "pga3dJS"),
      ProjectRef(file("../../"), "pga3dphysicsJS"),
      ProjectRef(file("../../"), "mathutilJS"),
    )
    .settings(
      scalacOptions -= "-Werror",
      scalacOptions -= "-Xfatal-warnings",
      name         := "indigodemo",
      version      := "0.0.1",
      scalaVersion := "3.8.3",
      organization := "com.github.kright",
      libraryDependencies ++= Seq(
        "org.scalameta" %%% "munit" % "1.1.1" % Test,
      ),
      testFrameworks += new TestFramework("munit.Framework"),
      scalafixOnCompile  := false,
      semanticdbEnabled  := true,
      semanticdbVersion  := scalafixSemanticdb.revision,
    )
    .settings( // Indigo specific settings
      indigoOptions := gameOptions,
      libraryDependencies ++= Seq(
        "io.indigoengine" %%% "indigo-json-circe" % "0.21.1",
        "io.indigoengine" %%% "indigo"            % "0.21.1",
        "io.indigoengine" %%% "indigo-extras"     % "0.21.1"
      ),
      Compile / sourceGenerators += Def.task {
        IndigoGenerators("com.github.kright.generated")
          .generateConfig("Config", gameOptions)
          .listAssets("Assets", gameOptions.assets)
          .toSourceFiles((Compile / sourceManaged).value)
      }
    )

lazy val indigo =
  (project in file("."))
    .settings(
      logo := "indigo-demo (v" + version.value.toString + ")",
      usefulTasks := Seq(
        UsefulTask("runGame", "Run the game").noAlias,
        UsefulTask("buildGame", "Build web version").noAlias,
        UsefulTask("runGameFull", "Run the fully optimised game").noAlias,
        UsefulTask("buildGameFull", "Build the fully optimised web version").noAlias
      ),
      logoColor        := scala.Console.MAGENTA,
      aliasColor       := scala.Console.YELLOW,
      commandColor     := scala.Console.CYAN,
      descriptionColor := scala.Console.WHITE
    )
    .aggregate(indigodemo)

addCommandAlias(
  "buildGame",
  List(
    "indigodemo/compile",
    "indigodemo/fastLinkJS",
    "indigodemo/indigoBuild"
  ).mkString(";", ";", "")
)
addCommandAlias(
  "buildGameFull",
  List(
    "indigodemo/compile",
    "indigodemo/fullLinkJS",
    "indigodemo/indigoBuildFull"
  ).mkString(";", ";", "")
)
addCommandAlias(
  "runGame",
  List(
    "indigodemo/compile",
    "indigodemo/fastLinkJS",
    "indigodemo/indigoRun"
  ).mkString(";", ";", "")
)
addCommandAlias(
  "runGameFull",
  List(
    "indigodemo/compile",
    "indigodemo/fullLinkJS",
    "indigodemo/indigoRunFull"
  ).mkString(";", ";", "")
)
