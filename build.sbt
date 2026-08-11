import pl.project13.scala.sbt.JmhPlugin

ThisBuild / organization := "me.kright"
ThisBuild / version := "0.10.0"
ThisBuild / scalaVersion := "3.8.4"
ThisBuild / pomIncludeRepository := { _ => false }

ThisBuild / description := "Kright Game Tools for Scala"
ThisBuild / homepage := Some(url("https://github.com/kright/KrightGameTools"))
ThisBuild / startYear := Some(2022)

ThisBuild / developers := List(
  Developer(
    id = "Kright",
    name = "Igor Slobodskov",
    email = "simplicivy@gmail.com",
    url = url("https://kright.me/about")
  )
)

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/kright/KrightGameTools"),
    "scm:git@github.com:kright/KrightGameTools.git"
  )
)

ThisBuild / licenses := List("MIT" -> url("https://opensource.org/licenses/MIT"))

ThisBuild / sonatypeCredentialHost := "central.sonatype.com"
ThisBuild / sonatypeRepository := "https://central.sonatype.com/service/local"
ThisBuild / sonatypeProfileName := "me.kright"

lazy val sonatypeSettings = Seq(
  publishMavenStyle := true,
  publishTo := {
    val centralSnapshots = "https://central.sonatype.com/repository/maven-snapshots/"
    if (isSnapshot.value) Some("central-snapshots" at centralSnapshots)
    else sonatypePublishToBundle.value
  }
)

lazy val explicitNulls =
  scalacOptions += "-Yexplicit-nulls"

lazy val wError =
  scalacOptions += "-Werror"

lazy val strictEquality =
  scalacOptions += "-language:strictEquality"

lazy val strictSettings = Seq(explicitNulls, wError, strictEquality)

lazy val scalatestSettings = Seq(
  // %%% (not %%) so the JS halves of the cross projects get the sjs artifacts; with the JVM
  // artifacts the JS test code compiles anyway (against TASTy), but cannot be linked or run
  libraryDependencies ++= Seq(
    "org.scalatest" %%% "scalatest" % "3.2.19" % "test",
    "org.scalatestplus" %%% "scalacheck-1-18" % "3.2.19.0" % "test"
  ),
  Test / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-S", "42"),
)

// the JVM is the primary platform: the JS halves compile their test code, but `sbt test` does
// not execute it, so the tests run without Node installed (suites in shared/src/test run on the
// JVM half). CI additionally links the JS test code (matrixJS/Test/fastLinkJS) - the linker
// needs no JS runtime and catches references to methods missing from the Scala.js javalib,
// like the Math.fma incident. To actually run a JS suite: `<module>JS/testOnly *SuiteName`.
lazy val jsTestsCompileNotRun = Seq(
  Test / test := { val _ = (Test / compile).value }
)

lazy val root = (project in file("."))
  .settings(
    name := "gametools",
    publish / skip := true,
  ).aggregate(
    symbolic,
    pgaNdCodeGen,
    mathutil.jvm, mathutil.js,
    flatarray.jvm, flatarray.js,
    vector.jvm, vector.js,
    ga,
    matrix.jvm, matrix.js,
    pga3d.jvm, pga3d.js,
    pga2d.jvm, pga2d.js,
    pga3dgeom.jvm, pga3dgeom.js,
    pga2dgeom.jvm, pga2dgeom.js,
    pga3dphysics.jvm, pga3dphysics.js,
    benchmark,
  )

lazy val mathutil = crossProject(JSPlatform, JVMPlatform)
  .withoutSuffixFor(JVMPlatform)
  .in(file("mathutil"))
  .jsSettings(jsTestsCompileNotRun)
  .settings(scalatestSettings, strictSettings)
  .settings(sonatypeSettings, name := "gametools-mathutil")

lazy val flatarray = crossProject(JSPlatform, JVMPlatform)
  .withoutSuffixFor(JVMPlatform)
  .in(file("flatarray"))
  .jsSettings(jsTestsCompileNotRun)
  .settings(scalatestSettings, strictSettings)
  .settings(sonatypeSettings, name := "gametools-flatarray")
  .dependsOn(mathutil)

lazy val vector = crossProject(JSPlatform, JVMPlatform)
  .withoutSuffixFor(JVMPlatform)
  .in(file("vector"))
  .jsSettings(jsTestsCompileNotRun)
  .settings(scalatestSettings, strictSettings)
  .settings(sonatypeSettings, name := "gametools-vector")
  .dependsOn(mathutil)

lazy val matrix = crossProject(JSPlatform, JVMPlatform)
  .withoutSuffixFor(JVMPlatform)
  .in(file("matrix"))
  .jsSettings(jsTestsCompileNotRun)
  .settings(strictSettings)
  .settings(
    libraryDependencies += "me.kright" %%% "arrayview" % "0.3.2",
  )
  .settings(scalatestSettings)
  .settings(sonatypeSettings, name := "gametools-matrix")
  .dependsOn(mathutil)

lazy val symbolic = (project in file("symbolic"))
  .settings(scalatestSettings, strictSettings)
  .settings(publish / skip := true)

lazy val ga = (project in file("ga"))
  .settings(scalatestSettings, strictSettings)
  .settings(publish / skip := true)
  .dependsOn(
    mathutil.jvm,
    symbolic % "test",
    vector.jvm % "compile->compile;test->test",
  )

lazy val pgaNdCodeGen = (project in file("pgaNdCodeGen"))
  .settings(scalatestSettings, strictSettings)
  .settings(publish / skip := true)
  .dependsOn(
    ga,
    symbolic,
  )

lazy val pga3d = crossProject(JSPlatform, JVMPlatform)
  .withoutSuffixFor(JVMPlatform)
  .in(file("pga3d"))
  .jsSettings(jsTestsCompileNotRun)
  .settings(scalatestSettings, strictSettings)
  .settings(sonatypeSettings, name := "gametools-pga3d")
  .dependsOn(
    matrix,
    mathutil,
    flatarray,
  )
  // the JVM tests check the generated closed forms (exp, dexp, ...) against the slow generic reference in ga
  .jvmConfigure(_.dependsOn(ga % "test"))

lazy val pga2d = crossProject(JSPlatform, JVMPlatform)
  .withoutSuffixFor(JVMPlatform)
  .in(file("pga2d"))
  .jsSettings(jsTestsCompileNotRun)
  .settings(scalatestSettings, strictSettings)
  .settings(sonatypeSettings, name := "gametools-pga2d")
  .dependsOn(
    mathutil,
    matrix,
    flatarray,
  )
  // the JVM tests check the generated closed forms (exp, dexp, ...) against the slow generic reference in ga
  .jvmConfigure(_.dependsOn(ga % "test"))

lazy val pga3dgeom = crossProject(JSPlatform, JVMPlatform)
  .withoutSuffixFor(JVMPlatform)
  .in(file("pga3dgeom"))
  .jsSettings(jsTestsCompileNotRun)
  .settings(scalatestSettings, strictSettings)
  .settings(sonatypeSettings, name := "gametools-pga3dgeom")
  .dependsOn(
    pga3d % "compile->compile;test->test",
    mathutil,
    matrix,
  )

lazy val pga2dgeom = crossProject(JSPlatform, JVMPlatform)
  .withoutSuffixFor(JVMPlatform)
  .in(file("pga2dgeom"))
  .jsSettings(jsTestsCompileNotRun)
  .settings(scalatestSettings, strictSettings)
  .settings(sonatypeSettings, name := "gametools-pga2dgeom")
  .dependsOn(
    pga2d % "compile->compile;test->test",
    mathutil,
    // 2d geometry is the z=0 special case of 3d: the correspondence is property-tested
    pga3dgeom % "test->compile",
  )

lazy val pga3dphysics = crossProject(JSPlatform, JVMPlatform)
  .withoutSuffixFor(JVMPlatform)
  .in(file("pga3dphysics"))
  .jsSettings(jsTestsCompileNotRun)
  .settings(scalatestSettings, strictSettings)
  .settings(sonatypeSettings, name := "gametools-pga3dphysics")
  .dependsOn(
    pga3d % "compile->compile;test->test",
    matrix,
  )

lazy val benchmark = (project in file("benchmark"))
  .enablePlugins(JmhPlugin)
  .settings(scalatestSettings, strictSettings)
  .settings(
    name := "gametools-benchmark",
    publish / skip := true,
    libraryDependencies += "org.openjdk.jmh" % "jmh-core" % "1.37",
    libraryDependencies += "org.openjdk.jmh" % "jmh-generator-annprocess" % "1.37",
  )
  .dependsOn(
    flatarray.jvm,
    pga3d.jvm,
    pga3dgeom.jvm,
    pga2dgeom.jvm,
    pga3dphysics.jvm,
  )