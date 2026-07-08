package me.kright.gametools.pga.codegen.scala.common.ops

import me.kright.gametools.pga.codegen.scala.common.{GeneratedCode, MultivectorUnaryOp, ScalaPgaAlgebra}

object DefMinMaxForPointOrVector:
  def apply()(using algebra: ScalaPgaAlgebra): MultivectorUnaryOp = MultivectorUnaryOp { (cls, _) =>
    GeneratedCode { code =>
      if (cls == algebra.point || cls == algebra.vector) {
        val names = cls.variableFields.map(_.name)
        val minArgs = names.map(n => s"math.min($n, other.$n)").mkString(", ")
        val maxArgs = names.map(n => s"math.max($n, other.$n)").mkString(", ")
        code(
          s"""
             |infix def min(other: ${cls.typeName}): ${cls.typeName} =
             |  ${cls.typeName}($minArgs)
             |
             |infix def max(other: ${cls.typeName}): ${cls.typeName} =
             |  ${cls.typeName}($maxArgs)
             |""".stripMargin
        )
      }
    }
  }
