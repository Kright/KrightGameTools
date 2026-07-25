package me.kright.gametools.pga.codegen.scalagen.pga3d

import Pga3dScalaAlgebra.pgaClasses
import me.kright.gametools.pga.codegen.common.{FileContent, GeneratedFileSystem, RealFileSystem}
import me.kright.gametools.pga.codegen.scalagen.common.OperationsReference

import java.nio.file.{Files, Path}


@main
def runScalaCodeGen(): Unit = {
  runScala3dCodeGen(RealFileSystem())
}

def runScala3dCodeGen(fs: GeneratedFileSystem): Unit = {
  val packageDir = Path.of("pga3d/shared/src/main/scala/me/kright/gametools/pga3d")
  assert(Files.exists(packageDir))

  for (cls <- pgaClasses if cls.shouldBeGenerated) {
    cls.writeToFile(packageDir, fs)
  }

  ScalaMatrixCodeGen().writeToFile(packageDir, fs)

  OperationsReference.writeToFile(Path.of("pga3d/operations.md"), fs)(using Pga3dScalaAlgebra)
}
