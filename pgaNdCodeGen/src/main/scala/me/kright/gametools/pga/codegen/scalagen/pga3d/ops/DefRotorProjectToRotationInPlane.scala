package me.kright.gametools.pga.codegen.scalagen.pga3d.ops

import me.kright.gametools.ga.PGA3
import me.kright.gametools.pga.codegen.common.{FormulaTemplate, SharedFormulas}
import me.kright.gametools.pga.codegen.scalagen.common.{GeneratedCode, MultivectorUnaryOp}
import me.kright.gametools.pga.codegen.scalagen.pga3d.Pga3dScalaAlgebra
import me.kright.gametools.pga.codegen.scalagen.pga3d.Pga3dScalaAlgebra.{planeCentral, rotor}

object DefRotorProjectToRotationInPlane:
  def apply()(using pga3: PGA3): MultivectorUnaryOp = MultivectorUnaryOp { (cls, v) =>
    GeneratedCode { code =>
      if (cls == rotor) {
        val prefix = Pga3dScalaAlgebra.typeNamePrefix

        code(s"\ndef projectToRotationInPlane(plane: ${planeCentral.name}): ${cls.name} =")
        code.block {
          code(FormulaTemplate.renderScala(SharedFormulas.rotorProjectToRotationInPlane, prefix))
        }

        code(s"\ndef restoreRotationInPlane(plane: ${planeCentral.name}): Double =")
        code.block {
          code(FormulaTemplate.renderScala(SharedFormulas.rotorRestoreRotationInPlane, prefix))
        }

        code(
          s"""
             |def restoreRotationInPlaneX: Double =
             |  restoreRotationInPlane(${planeCentral.name}(1, 0, 0))
             |
             |def restoreRotationInPlaneY: Double =
             |  restoreRotationInPlane(${planeCentral.name}(0, 1, 0))
             |
             |def restoreRotationInPlaneZ: Double =
             |  restoreRotationInPlane(${planeCentral.name}(0, 0, 1))""".stripMargin)
      }
    }
  }
