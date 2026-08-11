package me.kright.gametools.pga.codegen.scalagen.pga2d

import Pga2dScalaAlgebra.pgaClasses
import me.kright.gametools.pga.codegen.common.{GeneratedFileSystem, RealFileSystem}
import me.kright.gametools.pga.codegen.scalagen.common.{OperationsReference, ScalaMatrixCodeGen}

import java.nio.file.{Files, Path}


@main
def runScalaCodeGen(): Unit = {
  runScala2dCodeGen(RealFileSystem())
}

def runScala2dCodeGen(fs: GeneratedFileSystem): Unit = {
  val packageDir = Path.of("pga2d/shared/src/main/scala/me/kright/gametools/pga2d")
  require(Files.exists(packageDir), s"run from the repository root; not found: $packageDir")

  for (cls <- pgaClasses if cls.shouldBeGenerated) {
    cls.writeToFile(packageDir, fs)
  }

  ScalaMatrixCodeGen(Pga2dScalaAlgebra.projectivePoint)(using Pga2dScalaAlgebra).writeToFile(packageDir, fs)

  Pga2dTransformCodeGen().writeToFile(packageDir, fs)

  OperationsReference.writeToFile(Path.of("pga2d/operations.md"), fs)(using Pga2dScalaAlgebra)
}
