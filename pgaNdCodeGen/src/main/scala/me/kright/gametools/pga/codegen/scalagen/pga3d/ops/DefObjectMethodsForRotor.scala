package me.kright.gametools.pga.codegen.scalagen.pga3d.ops

import me.kright.gametools.ga.PGA3
import me.kright.gametools.pga.codegen.common.{FormulaTemplate, SharedFormulas}
import me.kright.gametools.pga.codegen.scalagen.common.{GeneratedCode, MultivectorUnaryOp}
import me.kright.gametools.pga.codegen.scalagen.pga3d.Pga3dScalaAlgebra
import me.kright.gametools.pga.codegen.scalagen.pga3d.Pga3dScalaAlgebra.{planeCentral, rotor, vector}

object DefObjectMethodsForRotor:
  def apply()(using pga3: PGA3): MultivectorUnaryOp =
    MultivectorUnaryOp { (cls, v) =>
      GeneratedCode { code =>
        if (cls == rotor) {
          code(s"\nval id: ${cls.typeName} = ${cls.typeName}(1.0, 0.0, 0.0, 0.0)")

          code(s"\ndef rotation(from: ${planeCentral.name}, to: ${planeCentral.name}): ${cls.name} = {")
          code.block {
            code("import me.kright.gametools.mathutil.ExactArith.diffOfProducts")
            code("")
            code(FormulaTemplate.renderScala(SharedFormulas.rotorRotation, Pga3dScalaAlgebra.typeNamePrefix))
          }
          code("}")

          code(
            s"""
               |def rotation(from: ${vector.name}, to: ${vector.name}): ${cls.name} =
               |  rotation(from.dual, to.dual)
               |
               |def fromAxes(axisX: ${vector.name}, axisY: ${vector.name}, axisZ: ${vector.name}): ${cls.name} = {
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
