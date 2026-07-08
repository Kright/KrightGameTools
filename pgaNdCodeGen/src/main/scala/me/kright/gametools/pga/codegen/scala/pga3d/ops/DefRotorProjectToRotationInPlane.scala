package me.kright.gametools.pga.codegen.scala.pga3d.ops

import me.kright.gametools.ga.PGA3
import me.kright.gametools.pga.codegen.scala.common.{GeneratedCode, MultivectorUnaryOp}
import me.kright.gametools.pga.codegen.scala.pga3d.Pga3dScalaAlgebra.rotor

object DefRotorProjectToRotationInPlane:
  def apply()(using pga3: PGA3): MultivectorUnaryOp = MultivectorUnaryOp { (cls, v) =>
    GeneratedCode { code =>
      if (cls == rotor) {
        code(
          s"""
             |def projectToRotationInPlane(plane: Pga3dPlaneIdeal): ${cls.name} =
             |  val q = this.normalizedByNorm
             |  val qPart = ${cls.name}.rotation(q.sandwich(plane), plane)
             |  qPart.geometric(q)
             |
             |def restoreRotationInPlane(plane: Pga3dPlaneIdeal): Double =
             |  val q0 = this.projectToRotationInPlane(plane)
             |  val logDual = q0.log().dual
             |  val currentAngle = 2.0 * (logDual.wx * plane.x + logDual.wy * plane.y + logDual.wz * plane.z) / plane.norm
             |  currentAngle
             |
             |def restoreRotationInPlaneX: Double =
             |  restoreRotationInPlane(Pga3dPlaneIdeal(1, 0, 0))
             |
             |def restoreRotationInPlaneY: Double =
             |  restoreRotationInPlane(Pga3dPlaneIdeal(0, 1, 0))
             |
             |def restoreRotationInPlaneZ: Double =
             |  restoreRotationInPlane(Pga3dPlaneIdeal(0, 0, 1))""".stripMargin)
      }
    }
  }
