package me.kright.gametools.pga.codegen.scala.pga2d

import Pga2dScalaAlgebra.pgaClasses
import me.kright.gametools.pga.codegen.common.{GeneratedFileSystem, RealFileSystem}
import me.kright.gametools.pga.codegen.scala.common.OperationsReference

import java.nio.file.{Files, Path}


@main
def runScalaCodeGen(): Unit = {
  runScala2dCodeGen(RealFileSystem())
}

def runScala2dCodeGen(fs: GeneratedFileSystem): Unit = {
  val packageDir = Path.of("pga2d/shared/src/main/scala/me/kright/gametools/pga2d")
  assert(Files.exists(packageDir))

  for (cls <- pgaClasses if cls.shouldBeGenerated) {
    cls.writeToFile(packageDir, fs)
  }

  // matrix generation is future work, intentionally skipped for pga2d

  OperationsReference.writeToFile(Path.of("pga2d/operations.md"), fs)(using Pga2dScalaAlgebra)
}
