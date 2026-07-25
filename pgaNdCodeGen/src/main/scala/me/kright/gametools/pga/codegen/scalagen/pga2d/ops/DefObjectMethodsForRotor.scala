package me.kright.gametools.pga.codegen.scalagen.pga2d.ops

import me.kright.gametools.ga.PGA2
import me.kright.gametools.pga.codegen.scalagen.common.{GeneratedCode, MultivectorUnaryOp}
import me.kright.gametools.pga.codegen.scalagen.pga2d.Pga2dScalaAlgebra.{lineCentral, rotor, vector}

object DefObjectMethodsForRotor:
  def apply()(using pga2: PGA2): MultivectorUnaryOp =
    MultivectorUnaryOp { (cls, v) =>
      GeneratedCode { code =>
        if (cls == rotor) {
          code(
            s"""
               |val id: ${cls.typeName} = ${cls.typeName}(1.0, 0.0)
               |
               |/** exp of the rotation generator xy (the half-angle of the rotation): the inverse of rotor.log */
               |def exp(xy: Double): ${cls.name} =
               |  ${cls.name}(Math.cos(xy), Math.sin(xy))
               |
               |def rotation(from: ${lineCentral.name}, to: ${lineCentral.name}): ${cls.name} = {
               |  // not Math.sqrt(from.normSquare * to.normSquare): the product overflows/underflows
               |  // for extreme magnitudes (~1e100 or ~1e-100) where each norm alone is still fine
               |  val norm = from.norm * to.norm
               |  val r2a = to.geometric(from) / norm
               |  val dot = r2a.s
               |
               |  // the -0.9 threshold keeps (1.0 + dot) >= 0.1, so the half-angle branch loses
               |  // at most ~2e-15 relative to the dot rounding; angles closer to pi take the
               |  // exact-wedge branch below, which stays ~1e-15 all the way to pi
               |  if (dot > -0.9) {
               |    val newCos = Math.sqrt((1.0 + dot) / 2.0)
               |    val newSinDivSin2 = 0.5 / newCos
               |    return ${cls.name}(newCos, r2a.xy * newSinDivSin2)
               |  }
               |
               |  // nearly a rotation by pi, the full angle is pi - eps. The wedge r2a.xy cancels
               |  // catastrophically near pi (~1e-17 absolute noise), so it is recomputed with
               |  // error-free products; the dot guard bounds |wedge| <= sin(acos(0.9)) ~ 0.44,
               |  // where asin is well-conditioned - unlike atan2 near pi, whose ~ulp(pi)
               |  // absolute error would be ~1e-16/eps relative in s.
               |  // Exactly antipodal inputs (wedge == 0) give the exact pi rotor (0, 1)
               |  import me.kright.gametools.mathutil.ExactArith.diffOfProducts
               |  val wedge = diffOfProducts(from.y, to.x, from.x, to.y) / norm
               |  val eps = Math.asin(Math.abs(wedge))
               |  val xy = if (wedge < 0.0) -Math.cos(eps * 0.5) else Math.cos(eps * 0.5)
               |  ${cls.name}(Math.sin(eps * 0.5), xy)
               |}
               |
               |def rotation(from: ${vector.name}, to: ${vector.name}): ${cls.name} =
               |  rotation(from.dual, to.dual)
               |
               |/** restore rotor from a rotated orthonormal basis (columns of a 2x2 rotation matrix),
               | *  so that restored.sandwich(${vector.name}(1, 0)) == axisX and restored.sandwich(${vector.name}(0, 1)) == axisY.
               | *  axisY is redundant in 2d (SO(2) has one degree of freedom), it is used only to symmetrize rounding errors,
               | *  in the same way as Pga3dRotor.fromAxes uses (m01 - m10) */
               |def fromAxes(axisX: ${vector.name}, axisY: ${vector.name}): ${cls.name} = {
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
