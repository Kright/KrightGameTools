package me.kright.gametools.pga.codegen.scalagen.pga3d.ops

import me.kright.gametools.ga.PGA3
import me.kright.gametools.pga.codegen.scalagen.common.{GeneratedCode, MultivectorUnaryOp}
import me.kright.gametools.pga.codegen.scalagen.pga3d.Pga3dScalaAlgebra.{planeCentral, rotor, vector}

object DefObjectMethodsForRotor:
  def apply()(using pga3: PGA3): MultivectorUnaryOp =
    MultivectorUnaryOp { (cls, v) =>
      GeneratedCode { code =>
        if (cls == rotor) {
          code(
            s"""
               |val id: ${cls.typeName} = ${cls.typeName}(1.0, 0.0, 0.0, 0.0)
               |
               |def rotation(from: ${planeCentral.name}, to: ${planeCentral.name}): ${cls.name} = {
               |  // not Math.sqrt(from.normSquare * to.normSquare): the product overflows/underflows
               |  // for extreme magnitudes (~1e100 or ~1e-100) where each norm alone is still fine
               |  val norm = from.norm * to.norm
               |  val q2a = to.geometric(from) / norm
               |  val dot = q2a.s
               |
               |  // the -0.9 threshold keeps (1.0 + dot) >= 0.1, so the half-angle branch loses
               |  // at most ~2e-15 relative to the dot rounding; angles closer to pi take the
               |  // exact-wedge branch below, which stays ~1e-15 all the way to pi
               |  if (dot > -0.9) {
               |    val newCos = Math.sqrt((1.0 + dot) / 2)
               |    val newSinDivSin2 = 0.5 / newCos
               |    return ${cls.name}(newCos, q2a.xy * newSinDivSin2, q2a.xz * newSinDivSin2, q2a.yz * newSinDivSin2)
               |  }
               |
               |  // near pi the wedge components of q2a cancel catastrophically (~1e-17 absolute
               |  // noise, which would tilt the axis by ~1e-17/sin2a), so the axis is recomputed
               |  // with error-free products
               |  import me.kright.gametools.mathutil.ExactArith.diffOfProducts
               |  val invNorm = 1.0 / norm
               |  val bxy = diffOfProducts(from.y, to.x, from.x, to.y) * invNorm
               |  val bxz = diffOfProducts(from.z, to.x, from.x, to.z) * invNorm
               |  val byz = diffOfProducts(from.z, to.y, from.y, to.z) * invNorm
               |  val sin2a = Math.sqrt(bxy * bxy + bxz * bxz + byz * byz)
               |
               |  if (sin2a > 0.0) {
               |    // rotation by (pi - eps): the dot guard bounds sin2a <= sin(acos(0.9)) ~ 0.44,
               |    // where asin is well-conditioned - unlike atan2 near pi, whose ~ulp(pi)
               |    // absolute error would be ~1e-16/eps relative in s
               |    val eps = Math.asin(sin2a)
               |    val axisMult = Math.cos(eps * 0.5) / sin2a
               |    return ${cls.name}(Math.sin(eps * 0.5), bxy * axisMult, bxz * axisMult, byz * axisMult)
               |  }
               |
               |  // exactly antipodal inputs: the axis is any direction orthogonal to from
               |  val orthogonalPlane =
               |    if (Math.abs(from.x) > Math.abs(from.z)) ${planeCentral.name}(-from.y, from.x, 0)
               |    else ${planeCentral.name}(0, -from.z, from.y)
               |
               |  ${cls.name}(0, orthogonalPlane.z, -orthogonalPlane.y, orthogonalPlane.x).normalizedByNorm
               |}
               |
               |def rotation(from: ${vector.name}, to: ${vector.name}): ${cls.name} =
               |  rotation(from.dual, to.dual)
               |
               |def restore(axisX: ${vector.name}, axisY: ${vector.name}, axisZ: ${vector.name}): ${cls.name} = {
               |  val m00 = axisX.x
               |  val m10 = axisX.y
               |  val m20 = axisX.z
               |  val m01 = axisY.x
               |  val m11 = axisY.y
               |  val m21 = axisY.z
               |  val m02 = axisZ.x
               |  val m12 = axisZ.y
               |  val m22 = axisZ.z
               |
               |  val tr = m00 + m11 + m22
               |  val max = Math.max(tr, Math.max(m00, Math.max(m11, m22)))
               |
               |  if (tr == max) {
               |    val s = Math.sqrt(1.0 + tr)
               |    val invS = 0.5 / s
               |    ${cls.name}(
               |      s = 0.5 * s,
               |      xy = (m01 - m10) * invS,
               |      xz = (m02 - m20) * invS,
               |      yz = (m12 - m21) * invS
               |    )
               |  } else if (m00 == max) {
               |    val yz = Math.sqrt(1.0 + m00 - m11 - m22)
               |    val invYZ = 0.5 / yz
               |    ${cls.name}(
               |      s = (m12 - m21) * invYZ,
               |      xy = (m20 + m02) * invYZ,
               |      xz = -(m10 + m01) * invYZ,
               |      yz = 0.5 * yz
               |    )
               |  } else if (m11 == max) {
               |    val xz = Math.sqrt(1.0 - m00 + m11 - m22)
               |    val invXZ = 0.5 / xz
               |    ${cls.name}(
               |      s = (m02 - m20) * invXZ,
               |      xy = -(m12 + m21) * invXZ,
               |      xz = 0.5 * xz,
               |      yz = -(m10 + m01) * invXZ
               |    )
               |  } else {
               |    val xy = Math.sqrt(1.0 - m00 - m11 + m22)
               |    val invXY = 0.5 / xy
               |    ${cls.name}(
               |      s = (m01 - m10) * invXY,
               |      xy = 0.5 * xy,
               |      xz = -(m12 + m21) * invXY,
               |      yz = (m20 + m02) * invXY
               |    )
               |  }
               |}
               |""".stripMargin)
        }
      }
    }
