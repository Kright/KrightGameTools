package me.kright.gametools.pga3d.codegen.scala

import ScalaMultivectorSubClass.pgaClasses

import java.nio.file.{Files, Path}


@main
def runScalaCodeGen(): Unit = {
  val packageDir = Path.of("pga3d/shared/src/main/scala/me/kright/gametools/pga3d")
  assert(Files.exists(packageDir))

  for (cls <- pgaClasses if cls.shouldBeGenerated) {
    cls.writeToFile(packageDir)
  }

  ScalaMatrixCodeGen().writeToFile(packageDir)
}