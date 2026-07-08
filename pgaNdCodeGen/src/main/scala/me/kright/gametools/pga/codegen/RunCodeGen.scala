package me.kright.gametools.pga.codegen

import me.kright.gametools.pga.codegen.common.{CheckFileSystem, RealFileSystem}
import me.kright.gametools.pga.codegen.scala.pga3d.runScala3dCodeGen
import me.kright.gametools.pga.codegen.scala.pga2d.runScala2dCodeGen
import me.kright.gametools.pga.codegen.cpp3d.runCppCodeGen

@main
def runCodeGen(): Unit = {
  val fs = RealFileSystem()
  runScala3dCodeGen(fs)
  runScala2dCodeGen(fs)
  runCppCodeGen(fs)
}

@main
def runCodeGenCheck(): Unit = {
  val fs = CheckFileSystem()
  runScala3dCodeGen(fs)
  runScala2dCodeGen(fs)
  runCppCodeGen(fs)
  fs.printSummary()
  if (fs.hasChanges) {
    System.exit(1)
  }
}
