package me.kright.gametools.pga.codegen.scalagen.pga3d.ops

import me.kright.gametools.ga.PGA3
import me.kright.gametools.pga.codegen.common.{FormulaTemplate, SharedFormulas}
import me.kright.gametools.pga.codegen.scalagen.common.{GeneratedCode, MultivectorUnaryOp}
import me.kright.gametools.pga.codegen.scalagen.pga3d.Pga3dScalaAlgebra
import me.kright.gametools.pga.codegen.scalagen.pga3d.Pga3dScalaAlgebra.{bivector, bivectorWeight}

object DefBivectorSplit:
  def apply()(using pga3: PGA3): MultivectorUnaryOp = MultivectorUnaryOp { (cls, v) =>
    if (cls == bivector) {
      GeneratedCode { code =>
        val self = cls.self
        val prefix = Pga3dScalaAlgebra.typeNamePrefix

        code(s"\ndef split: (${bivector.name}, ${bivectorWeight.name}) =")
        code.block {
          code(FormulaTemplate.renderScala(SharedFormulas.bivectorSplitGuard, prefix))
          code.block {
            code(s"return (${bivector.name}(0.0, 0.0, 0.0, xy, xz, yz), ${bivectorWeight.name}(wx, wy, wz))")
          }
          code("}")
          code("")
          code(FormulaTemplate.renderScala(SharedFormulas.bivectorSplitPseudoScalar(self), prefix))
          code(s"val shiftAlongLine = ${bivectorWeight.makeConstructor(SharedFormulas.bivectorSplitShift(self))}")
          code("")
          code("val line = this - shiftAlongLine")
          code("(line, shiftAlongLine)")
        }
      }
    } else None
  }
