package me.kright.gametools.pga.codegen.scalagen.pga3d

import Pga3dScalaAlgebra.pgaClasses
import me.kright.gametools.pga.codegen.common.{FileContent, GeneratedFileSystem, RealFileSystem}
import me.kright.gametools.pga.codegen.scalagen.common.{OperationsReference, ScalaMatrixCodeGen}

import java.nio.file.{Files, Path}


@main
def runScalaCodeGen(): Unit = {
  runScala3dCodeGen(RealFileSystem())
}

def runScala3dCodeGen(fs: GeneratedFileSystem): Unit = {
  val packageDir = Path.of("pga3d/shared/src/main/scala/me/kright/gametools/pga3d")
  require(Files.exists(packageDir), s"run from the repository root; not found: $packageDir")

  for (cls <- pgaClasses if cls.shouldBeGenerated) {
    cls.writeToFile(packageDir, fs)
  }

  ScalaMatrixCodeGen(Pga3dScalaAlgebra.bivector)(using Pga3dScalaAlgebra).writeToFile(packageDir, fs)

  Pga3dTransformCodeGen(normalized = false).writeToFile(packageDir, fs)
  Pga3dTransformCodeGen(normalized = true).writeToFile(packageDir, fs)

  OperationsReference.writeToFile(Path.of("pga3d/operations.md"), fs)(using Pga3dScalaAlgebra)
}
