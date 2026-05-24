package me.kright.gametools.pga3d.codegen

import me.kright.gametools.pga3d.codegen.scala.runScalaCodeGen
import me.kright.gametools.pga3d.codegen.cpp.runCppCodeGen

@main
def runCodeGen(): Unit = {
  runScalaCodeGen()
  runCppCodeGen()
}
