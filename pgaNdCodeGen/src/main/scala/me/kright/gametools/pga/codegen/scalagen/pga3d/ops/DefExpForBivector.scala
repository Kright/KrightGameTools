package me.kright.gametools.pga.codegen.scalagen.pga3d.ops

import me.kright.gametools.ga.PGA3
import me.kright.gametools.pga.codegen.common.{FormulaTemplate, SharedFormulas}
import me.kright.gametools.pga.codegen.scalagen.common.{GeneratedCode, MultivectorUnaryOp}
import me.kright.gametools.pga.codegen.scalagen.pga3d.Pga3dScalaAlgebra
import me.kright.gametools.pga.codegen.scalagen.pga3d.Pga3dScalaAlgebra.{bivector, bivectorBulk, bivectorWeight, motor, rotor, translator}
import me.kright.gametools.symbolic.Sym

object DefExpForBivector:
  def apply()(using pga3: PGA3): MultivectorUnaryOp = MultivectorUnaryOp { (cls, v) =>
    GeneratedCode { code =>
      val self = cls.self
      val prefix = Pga3dScalaAlgebra.typeNamePrefix

      if (cls == bivector) {
        code(s"\ndef exp: ${motor.name} =")
        code.block {
          code(FormulaTemplate.renderScala(SharedFormulas.expSinDivLen("bulkNorm"), prefix))
          code("")
          code(FormulaTemplate.renderScala(SharedFormulas.expSinMinusCos, prefix))
          code("")
          code(motor.makeConstructor(SharedFormulas.bivectorExpResult(self)))
        }

        code(s"\n/** for components below ~1e-154 the pairwise field products of this t-factored form may underflow; (b * t).exp does not */")
        code(s"def exp(t: Double): ${motor.name} =")
        code.block {
          code(FormulaTemplate.renderScala(SharedFormulas.expSinDivLen("bulkNorm * abs(t)"), prefix))
          code("")
          code(FormulaTemplate.renderScala(SharedFormulas.expSinMinusCos, prefix))
          code("")
          code(motor.makeConstructor(SharedFormulas.bivectorExpResult(self * Sym("t"))))
        }
      }
      if (cls == bivectorBulk) {
        code(s"\ndef exp: ${rotor.typeName} =")
        code.block {
          code(FormulaTemplate.renderScala(SharedFormulas.expSinDivLen("bulkNorm"), prefix))
          code("")
          code(rotor.makeConstructor(SharedFormulas.bivectorExpResult(self)))
        }

        code(s"\ndef exp(t: Double): ${rotor.typeName} =")
        code.block {
          code(FormulaTemplate.renderScala(SharedFormulas.expSinDivLen("bulkNorm * abs(t)"), prefix))
          code("")
          code(rotor.makeConstructor(SharedFormulas.bivectorExpResult(self * Sym("t"))))
        }
      }
      if (cls == bivectorWeight) {
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
