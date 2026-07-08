package me.kright.gametools.pga.codegen.scala.pga2d.ops

import me.kright.gametools.ga.PGA2
import me.kright.gametools.pga.codegen.scala.common.{GeneratedCode, MultivectorUnaryOp}
import me.kright.gametools.pga.codegen.scala.pga2d.Pga2dScalaAlgebra.{motor, projectivePoint, translator, vector}
import me.kright.gametools.symbolic.Sym

object DefLogForMotor:
  def apply()(using pga2: PGA2): MultivectorUnaryOp = MultivectorUnaryOp { (cls, v) =>
    GeneratedCode { code =>
      val self = cls.self

      if (cls == motor) {
        val vb = self.grade(2)
        val result = vb * Sym("b")

        code(s"\ndef log(): ${projectivePoint.typeName} =")
        code.block {
          code(
            s"""val scalar = s
               |if (s < 0.0) return (-this).log()
               |
               |val lenXY = Math.abs(xy)
               |val angle = Math.atan2(lenXY, scalar)
               |
               |// 1 / sin^2 for a normalized motor; (1.0 - scalar * scalar) is the same value,
               |// but cancels catastrophically for small angles (relative error ~eps / angle^2)
               |val a = 1.0 / (lenXY * lenXY)
               |
               |val b = if (Math.abs(angle) > 1e-5) { // angle / sin(angle)
               |  angle * Math.sqrt(a)
               |} else {
               |  // x/sin(x) = 1 + x^2/6 + 7x^4/360 + ...; at x <= 1e-5 the dropped 7x^4/360 <= 2e-22
               |  // relative term is far below 2^-53, so the second-order form is exact in double
               |  1.0 + angle * angle / 6.0
               |}
               |
               |${projectivePoint.makeConstructor(result)}
               |""".stripMargin)
        }
      }
      if (cls == translator) {
        code(s"\ndef log(): ${vector.typeName} =")
        code.block {
          code(vector.makeConstructor(self.weight))
        }
      }
    }
  }
