package com.github.kright.pga3d.codegen.cpp.ops

import com.github.kright.pga3d.codegen.cpp.{CppCodeBuilder, CppSubclass, CppSubclasses}
import me.kright.gametools.symbolic.Sym

object QuaternionAndMotorAxes {
  def makeDeclaration(code: CppCodeBuilder, cls: CppSubclass): Unit = {
    for (methodName <- Seq("axisX", "axisY", "axisZ")) {
      code(s"[[nodiscard]] constexpr ${CppSubclasses.vector.name} ${methodName}() const noexcept;")
    }
  }

  def makeForQuaternion(code: CppCodeBuilder): Unit = {
    val self = CppSubclasses.quaternion.self
    val vec = CppSubclasses.vector.self
    val axes = vec.values.keys.toSeq.sortBy(_.bits).reverse.map(blade => vec.filter((b, _) => b == blade))

    for (axe <- axes) {
      val axeOne = axe.mapValues(_ => Sym(1.0))

      val isMinus = axe.values.values.head.toString.contains("-")
      val methodName = s"axis${axe.values.values.head.toString.replace("-", "").toUpperCase}"

      val result = if (isMinus) self.sandwich(axeOne) * Sym(-1.0) else self.sandwich(axeOne)
      val resultCls = CppSubclasses.findMatchingClass(result)

      require(resultCls == CppSubclasses.vector)

      code(s"[[nodiscard]] constexpr ${CppSubclasses.vector.name} ${CppSubclasses.quaternion.name}::${methodName}() const noexcept { return ${resultCls.makeBracesInit(result)}; }")
    }
  }

  def makeForMotor(code: CppCodeBuilder): Unit = {
    val self = CppSubclasses.quaternion.self
    val vec = CppSubclasses.vector.self
    val axes = vec.values.keys.toSeq.sortBy(_.bits).reverse.map(blade => vec.filter((b, _) => b == blade))

    code(
      s"""[[nodiscard]] constexpr ${CppSubclasses.vector.name} ${CppSubclasses.motor.name}::axisX() const noexcept { return toQuaternionUnsafe().axisX(); }
         |[[nodiscard]] constexpr ${CppSubclasses.vector.name} ${CppSubclasses.motor.name}::axisY() const noexcept { return toQuaternionUnsafe().axisY(); }
         |[[nodiscard]] constexpr ${CppSubclasses.vector.name} ${CppSubclasses.motor.name}::axisZ() const noexcept { return toQuaternionUnsafe().axisZ(); }
         |""".stripMargin
    )
  }
}
