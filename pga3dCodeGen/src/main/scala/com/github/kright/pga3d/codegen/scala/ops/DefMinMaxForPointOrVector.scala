package com.github.kright.pga3d.codegen.scala.ops

import me.kright.gametools.ga.PGA3
import com.github.kright.pga3d.codegen.scala.{GeneratedCode, ScalaMultivectorSubClass, MultivectorUnaryOp}

object DefMinMaxForPointOrVector:
  def apply()(using pga3: PGA3): MultivectorUnaryOp = MultivectorUnaryOp { (cls, s) =>
    GeneratedCode { code =>
      if (cls == ScalaMultivectorSubClass.point || cls == ScalaMultivectorSubClass.vector) {
        code(
          s"""
             |infix def min(other: ${cls.typeName}): ${cls.typeName} =
             |  ${cls.typeName}(math.min(x, other.x), math.min(y, other.y), math.min(z, other.z))
             |
             |infix def max(other: ${cls.typeName}): ${cls.typeName} =
             |  ${cls.typeName}(math.max(x, other.x), math.max(y, other.y), math.max(z, other.z))
             |""".stripMargin
        )
      }
    }
  }
