package me.kright.gametools.pga.codegen.scalagen.pga2d.ops

import me.kright.gametools.ga.PGA2
import me.kright.gametools.pga.codegen.scalagen.common.{GeneratedCode, MultivectorUnaryOp}
import me.kright.gametools.pga.codegen.scalagen.pga2d.Pga2dScalaAlgebra.{motor, projectivePoint, rotor, translator, vector}
import me.kright.gametools.symbolic.Sym

object DefLogForMotor:
  def apply()(using pga2: PGA2): MultivectorUnaryOp = MultivectorUnaryOp { (cls, v) =>
    GeneratedCode { code =>
      val self = cls.self

      if (cls == motor) {
        val vb = self.grade(2)
        val result = vb * Sym("b")

        code(s"\ndef log: ${projectivePoint.typeName} =")
        code.block {
          code(
            s"""val scalar = s
               |if (s < 0.0) return (-this).log
               |
               |val lenXY = Math.abs(xy)
               |val angle = Math.atan2(lenXY, scalar)
               |
               |// for a normalized motor sin(angle) = lenXY, so this is angle / sin(angle);
               |// dividing by lenXY directly avoids the catastrophic cancellation that the
               |// equivalent sqrt(1.0 - scalar * scalar) form has for small angles
               |val b = if (Math.abs(angle) > 1e-5) {
               |  angle / lenXY
               |} else {
               |  // x/sin(x) = 1 / (sin(x)/x) = 1 / (1 - x^2/6 + x^4/120 - ...);
               |  // substitute v = x^2/6 - x^4/120 + ... into 1/(1 - v) = 1 + v + v^2 + ...:
               |  //   x/sin(x) = 1 + x^2/6 + (1/36 - 1/120)*x^4 + ...
               |  //            = 1 + x^2/6 + 7*x^4/360 + ...
               |  // at x <= 1e-5 the dropped 7*x^4/360 <= 2e-22 relative term is far below 1e-17,
               |  // so the second-order form is exact in double
               |  1.0 + angle * angle / 6.0
               |}
               |
               |${projectivePoint.makeConstructor(result)}
               |""".stripMargin)
        }
      }
      if (cls == translator) {
        code(s"\ndef log: ${vector.typeName} =")
        code.block {
          code(vector.makeConstructor(self.weight))
        }
      }
      if (cls == rotor) {
        code(
          s"""
             |/**
             | * The xy coefficient of the rotation generator, i.e. the half-angle of the rotation.
             | * There is no single-blade grade-2 class in 2d, so the coefficient is returned as a
             | * plain Double; ${rotor.typeName}.exp is the inverse. Scale-invariant via atan2.
             | */
             |def log: Double =""".stripMargin)
        code.block {
          code(
            s"""if (s < 0.0) Math.atan2(-xy, -s)
               |else Math.atan2(xy, s)""".stripMargin)
        }
      }
    }
  }
