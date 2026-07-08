package me.kright.gametools.pga.codegen.scala.pga2d.ops

import me.kright.gametools.ga.PGA2
import me.kright.gametools.pga.codegen.scala.common.{GeneratedCode, MultivectorUnaryOp}
import me.kright.gametools.pga.codegen.scala.pga2d.Pga2dScalaAlgebra.{lineIdeal, rotor, vector}

object DefObjectMethodsForRotor:
  def apply()(using pga2: PGA2): MultivectorUnaryOp =
    MultivectorUnaryOp { (cls, v) =>
      GeneratedCode { code =>
        if (cls == rotor) {
          code(
            s"""
               |val id: ${cls.typeName} = ${cls.typeName}(1.0, 0.0)
               |
               |def rotation(from: ${lineIdeal.name}, to: ${lineIdeal.name}): ${cls.name} = {
               |  // not Math.sqrt(from.normSquare * to.normSquare): the product overflows/underflows
               |  // for extreme magnitudes (~1e100 or ~1e-100) where each norm alone is still fine
               |  val norm = from.norm * to.norm
               |  val r2a = to.geometric(from) / norm
               |  val dot = r2a.s
               |
               |  if (dot > -1.0 + 1e-6) {
               |    val newCos = Math.sqrt((1.0 + dot) / 2.0)
               |    val newSinDivSin2 = 0.5 / newCos
               |    return ${cls.name}(newCos, r2a.xy * newSinDivSin2)
               |  }
               |
               |  val sin2a = Math.abs(r2a.xy)
               |  if (sin2a > 1e-8) {
               |    val angle2 = Math.atan2(sin2a, r2a.s)
               |    val propAngle = angle2 * 0.5
               |    val mult = Math.sin(propAngle) / sin2a
               |    return ${cls.name}(Math.cos(propAngle), r2a.xy * mult).normalizedByNorm
               |  }
               |
               |  // nearly a rotation by pi: the full angle is pi - eps with sin(eps) = sin2a <= 1e-8.
               |  // first-order half-angle rotor: s = sin(eps/2) = 0.5 * sin2a * (1 + eps^2/8 + O(eps^4)) and
               |  // xy = +-cos(eps/2) = +-(1 - eps^2/8); at eps <= 1e-8 the dropped eps^2/8 <= 1.25e-17 terms
               |  // are below 2^-53, so both components are exact in double. xy carries the sign of r2a.xy,
               |  // matching the atan2 branch above, so the result is continuous across the sin2a threshold;
               |  // sin2a == 0 (exactly antipodal inputs) gives the exact pi rotor (0, 1)
               |  ${cls.name}(0.5 * sin2a, if (r2a.xy < 0.0) -1.0 else 1.0)
               |}
               |
               |def rotation(from: ${vector.name}, to: ${vector.name}): ${cls.name} =
               |  rotation(from.dual, to.dual)
               |
               |/** restore rotor from a rotated orthonormal basis (columns of a 2x2 rotation matrix),
               | *  so that restored.sandwich(${vector.name}(1, 0)) == axisX and restored.sandwich(${vector.name}(0, 1)) == axisY.
               | *  axisY is redundant in 2d (SO(2) has one degree of freedom), it is used only to symmetrize rounding errors,
               | *  in the same way as Pga3dQuaternion.restore uses (m01 - m10) */
               |def restore(axisX: ${vector.name}, axisY: ${vector.name}): ${cls.name} = {
               |  val cosT = 0.5 * (axisX.x + axisY.y)
               |  val sinT = 0.5 * (axisY.x - axisX.y)
               |  // both branches give the same rotor; -0.9 is not a correctness boundary, it only picks
               |  // the better-conditioned half-angle form: (1 + cosT) loses precision near the angle pi
               |  if (cosT > -0.9) ${cls.name}(1.0 + cosT, sinT).normalizedByNorm
               |  else ${cls.name}(sinT, 1.0 - cosT).normalizedByNorm
               |}""".stripMargin)
        }
      }
    }
