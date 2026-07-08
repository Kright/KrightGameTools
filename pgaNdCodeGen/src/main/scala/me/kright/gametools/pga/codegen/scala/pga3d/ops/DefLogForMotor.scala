package me.kright.gametools.pga.codegen.scala.pga3d.ops

import me.kright.gametools.ga.PGA3
import me.kright.gametools.pga.codegen.scala.common.{GeneratedCode, MultivectorUnaryOp}
import me.kright.gametools.pga.codegen.scala.pga3d.Pga3dScalaAlgebra.{bivector, bivectorBulk, bivectorWeight, motor, rotor, translator}
import me.kright.gametools.symbolic.Sym

object DefLogForMotor:
  def apply()(using pga3: PGA3): MultivectorUnaryOp = MultivectorUnaryOp { (cls, v) =>
    GeneratedCode { code =>
      val self = cls.self

      if (cls == motor) {
        val vb = self.grade(2)
        val result = vb * Sym("b") + vb.bulk.dual * Sym("c")

        code(s"\ndef log(): ${bivector.typeName} =")
        code.block {
          code(
            s"""val scalar = s
               |if (s < 0.0) return (-this).log()
               |
               |val lenXYZ = Math.sqrt(xy * xy + xz * xz + yz * yz)
               |val angle = Math.atan2(lenXYZ, scalar)
               |
               |// 1 / sin^2 for a normalized motor; (1.0 - scalar * scalar) is the same value,
               |// but cancels catastrophically for small angles (relative error ~eps / angle^2)
               |val a = 1.0 / (lenXYZ * lenXYZ)
               |
               |val b = if (Math.abs(angle) > 1e-5) { // angle / sin(angle)
               |  angle * Math.sqrt(a)
               |} else {
               |  // x/sin(x) = 1 + x^2/6 + 7x^4/360 + ...; at x <= 1e-5 the dropped 7x^4/360 <= 2e-22
               |  // relative term is far below 2^-53, so the second-order form is exact in double
               |  1.0 + angle * angle / 6.0
               |}
               |
               |val c = if (Math.abs(angle) > 1e-5) {
               |  a * i * (1.0 - scalar * b)
               |} else {
               |  (1.0 + angle * angle / 2.0) * i / 3.0
               |}
               |
               |${bivector.makeConstructor(result)}
               |""".stripMargin)
        }
      }
      if (cls == translator) {
        code(s"\ndef log(): ${bivectorWeight.typeName} =")
        code.block {
          code(bivectorWeight.makeConstructor(self.weight))
        }
      }
      if (cls == rotor) {
        val vb = self.grade(2)
        val result = vb * Sym("b")

        code(s"\ndef log(): ${bivectorBulk.typeName} =")
        code.block {
          code(
            s"""val scalar = s
               |if (s < 0.0) return (-this).log()
               |
               |val lenXYZ = Math.sqrt(xy * xy + xz * xz + yz * yz)
               |val angle = Math.atan2(lenXYZ, scalar)
               |
               |// 1 / sin^2 for a normalized rotor; (1.0 - scalar * scalar) is the same value,
               |// but cancels catastrophically for small angles (relative error ~eps / angle^2)
               |val a = 1.0 / (lenXYZ * lenXYZ)
               |
               |val b = if (Math.abs(angle) > 1e-5) { // angle / sin(angle)
               |  angle * Math.sqrt(a)
               |} else {
               |  // x/sin(x) = 1 + x^2/6 + 7x^4/360 + ...; at x <= 1e-5 the dropped 7x^4/360 <= 2e-22
               |  // relative term is far below 2^-53, so the second-order form is exact in double
               |  1.0 + angle * angle / 6.0
               |}
               |
               |${bivectorBulk.makeConstructor(result)}
               |""".stripMargin)
        }
      }
    }
  }
