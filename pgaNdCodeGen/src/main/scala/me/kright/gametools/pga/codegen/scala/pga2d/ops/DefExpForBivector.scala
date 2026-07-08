package me.kright.gametools.pga.codegen.scala.pga2d.ops

import me.kright.gametools.ga.{MultiVector, PGA2}
import me.kright.gametools.pga.codegen.scala.common.{GeneratedCode, MultivectorUnaryOp}
import me.kright.gametools.pga.codegen.scala.pga2d.Pga2dScalaAlgebra.{motor, projectivePoint, translator, vector}
import me.kright.gametools.symbolic.Sym

object DefExpForBivector:
  def apply()(using pga2: PGA2): MultivectorUnaryOp = MultivectorUnaryOp { (cls, v) =>
    GeneratedCode { code =>
      val self = cls.self
      if (cls == projectivePoint) {
        // in 2d, bulk ^ weight = 0 for any grade-2 element, so B * B = -bulkNormSquare exactly
        // and exp(B) = cos(len) + B * sin(len) / len, without the pseudoscalar correction term of 3d
        {
          val result = MultiVector.scalar(Sym("cos")) + self * Sym("sinDivLen")

          code(s"\ndef exp(): ${motor.name} =")
          code.block {
            code(
              s"""val len = bulkNorm
                 |val cos = Math.cos(len)
                 |
                 |// sin(x)/x = 1 - x^2/6 + x^4/120 - ...; at x <= 1e-5 the dropped x^4/120 <= 8.4e-23
                 |// relative term is far below 2^-53, so the second-order form is exact in double
                 |val sinDivLen = if (len > 1e-5) {
                 |  Math.sin(len) / len
                 |} else 1.0 - (len * len) / 6.0
                 |
                 |${motor.makeConstructor(result)}""".stripMargin
            )
          }
        }

        {
          val selfMulT = self * Sym("t")
          val result = MultiVector.scalar(Sym("cos")) + selfMulT * Sym("sinDivLen")

          code(s"\ndef exp(t: Double): ${motor.name} =")
          code.block {
            code(
              s"""val len = bulkNorm * Math.abs(t)
                 |val cos = Math.cos(len)
                 |
                 |// sin(x)/x = 1 - x^2/6 + x^4/120 - ...; at x <= 1e-5 the dropped x^4/120 <= 8.4e-23
                 |// relative term is far below 2^-53, so the second-order form is exact in double
                 |val sinDivLen = if (len > 1e-5) {
                 |  Math.sin(len) / len
                 |} else 1.0 - (len * len) / 6.0
                 |
                 |${motor.makeConstructor(result)}""".stripMargin
            )
          }
        }
      }
      if (cls == vector) {
        {
          val result = MultiVector.scalar(Sym(1.0)) + self
          code(s"\ndef exp(): ${translator.typeName} =")
          code.block {
            code(translator.makeConstructor(result))
          }
        }

        {
          val result = MultiVector.scalar(Sym(1.0)) + self * Sym("t")
          code(s"\ndef exp(t: Double): ${translator.typeName} =")
          code.block {
            code(translator.makeConstructor(result))
          }
        }
      }
    }
  }
