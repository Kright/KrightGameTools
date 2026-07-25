package me.kright.gametools.pga.codegen.cpp3d.ops

import me.kright.gametools.pga.codegen.common.AxesSymbolics
import me.kright.gametools.pga.codegen.cpp3d.{CppCodeBuilder, CppSubclass, CppSubclasses}

import scala.collection.immutable.ArraySeq

object RotorAndMotorAxes {
  def makeDeclaration(code: CppCodeBuilder, cls: CppSubclass): Unit = {
    for (methodName <- ArraySeq("axisX", "axisY", "axisZ")) {
      code(s"[[nodiscard]] constexpr ${CppSubclasses.vector.name} ${methodName}() const noexcept;")
    }
  }

  def makeForRotor(code: CppCodeBuilder): Unit = {
    for (axis <- AxesSymbolics.rotorAxes(CppSubclasses.rotor.self, CppSubclasses.vector.self)) {
      val resultCls = CppSubclasses.findMatchingClass(axis.result)
      require(resultCls == CppSubclasses.vector)

      code(s"[[nodiscard]] constexpr ${CppSubclasses.vector.name} ${CppSubclasses.rotor.name}::${axis.methodName}() const noexcept { return ${resultCls.makeBracesInit(axis.result)}; }")
    }
  }

  def makeForMotor(code: CppCodeBuilder): Unit = {
    for (axis <- AxesSymbolics.rotorAxes(CppSubclasses.rotor.self, CppSubclasses.vector.self)) {
      code(s"[[nodiscard]] constexpr ${CppSubclasses.vector.name} ${CppSubclasses.motor.name}::${axis.methodName}() const noexcept { return toRotorUnsafe().${axis.methodName}(); }")
    }
  }
}
