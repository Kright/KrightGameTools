package me.kright.gametools.pga3d.codegen.scala.ops

import me.kright.gametools.ga.PGA3
import me.kright.gametools.pga3d.codegen.scala.ScalaMultivectorSubClass.{planeIdeal, quaternion, vector}
import me.kright.gametools.pga3d.codegen.scala.{GeneratedCode, MultivectorUnaryOp}

object DefObjectMethodsForQuaternion:
  def apply()(using pga3: PGA3): MultivectorUnaryOp =
    MultivectorUnaryOp { (cls, v) =>
      GeneratedCode { code =>
        if (cls == quaternion) {
          code(
            s"""
               |val id: ${cls.typeName} = ${cls.typeName}(1.0, 0.0, 0.0, 0.0)
               |
               |def rotation(from: ${planeIdeal.name}, to: ${planeIdeal.name}): ${cls.name} = {
               |  val norm = Math.sqrt(from.normSquare * to.normSquare)
               |  val q2a = to.geometric(from) / norm
               |  val dot = q2a.s
               |
               |  if (dot > -1.0 + 1e-6) {
               |    val newCos = Math.sqrt((1.0 + dot) / 2)
               |    val newSinDivSin2 = 0.5 / newCos
               |    return ${cls.name}(newCos, q2a.xy * newSinDivSin2, q2a.xz * newSinDivSin2, q2a.yz * newSinDivSin2)
               |  }
               |
               |  val sin2a = Math.sqrt(q2a.xy * q2a.xy + q2a.xz * q2a.xz + q2a.yz * q2a.yz)
               |
               |  if (sin2a > 1e-8) {
               |    val angle2 = Math.atan2(sin2a, q2a.s)
               |    val propAngle = angle2 * 0.5
               |    val mult = Math.sin(propAngle) / sin2a
               |    return ${cls.name}(Math.cos(propAngle), q2a.xy * mult, q2a.xz * mult, q2a.yz * mult).normalizedByNorm
               |  }
               |
               |  // choose any axis
               |  val orthogonalPlane =
               |    if (Math.abs(from.x) > Math.abs(from.z)) ${planeIdeal.name}(-from.y, from.x, 0)
               |    else ${planeIdeal.name}(0, -from.z, from.y)
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
