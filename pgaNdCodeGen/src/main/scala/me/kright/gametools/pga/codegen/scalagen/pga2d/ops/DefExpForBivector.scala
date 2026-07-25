package me.kright.gametools.pga.codegen.scalagen.pga2d.ops

import me.kright.gametools.ga.{MultiVector, PGA2}
import me.kright.gametools.pga.codegen.common.{FormulaTemplate, SharedFormulas}
import me.kright.gametools.pga.codegen.scalagen.common.{GeneratedCode, MultivectorUnaryOp}
import me.kright.gametools.pga.codegen.scalagen.pga2d.Pga2dScalaAlgebra
import me.kright.gametools.pga.codegen.scalagen.pga2d.Pga2dScalaAlgebra.{motor, projectivePoint, translator, vector}
import me.kright.gametools.symbolic.Sym

object DefExpForBivector:
  def apply()(using pga2: PGA2): MultivectorUnaryOp = MultivectorUnaryOp { (cls, v) =>
    GeneratedCode { code =>
      val self = cls.self
      val prefix = Pga2dScalaAlgebra.typeNamePrefix

      def pointExpResult(b: MultiVector[Sym]): MultiVector[Sym] =
        MultiVector.scalar(Sym("cos")) + b * Sym("sinDivLen")

      if (cls == projectivePoint) {
        // in 2d, bulk ^ weight = 0 for any grade-2 element, so B * B = -bulkNormSquare exactly
        // and exp(B) = cos(len) + B * sin(len) / len, without the pseudoscalar correction term of 3d
        code(s"\ndef exp: ${motor.name} =")
        code.block {
          code(FormulaTemplate.renderScala(SharedFormulas.expSinDivLen("bulkNorm"), prefix))
          code("")
          code(motor.makeConstructor(pointExpResult(self)))
        }

        code(s"\ndef exp(t: Double): ${motor.name} =")
        code.block {
          code(FormulaTemplate.renderScala(SharedFormulas.expSinDivLen("bulkNorm * abs(t)"), prefix))
          code("")
          code(motor.makeConstructor(pointExpResult(self * Sym("t"))))
        }
      }
      if (cls == vector) {
        code(s"\ndef exp: ${translator.typeName} =")
        code.block {
          code(translator.makeConstructor(SharedFormulas.weightExpResult(self)))
        }

        code(s"\ndef exp(t: Double): ${translator.typeName} =")
        code.block {
          code(translator.makeConstructor(SharedFormulas.weightExpResult(self * Sym("t"))))
        }
      }
    }
  }
