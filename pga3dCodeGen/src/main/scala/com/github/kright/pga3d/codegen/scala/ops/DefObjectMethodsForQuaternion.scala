package com.github.kright.pga3d.codegen.scala.ops

import me.kright.gametools.ga.PGA3
import com.github.kright.pga3d.codegen.scala.ScalaMultivectorSubClass.{planeIdeal, quaternion, vector}
import com.github.kright.pga3d.codegen.scala.{GeneratedCode, MultivectorUnaryOp}

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
               |""".stripMargin)
        }
      }
    }
