package me.kright.gametools.pga.codegen.scalagen.common.ops

import me.kright.gametools.pga.codegen.scalagen.common.{GeneratedCode, MultivectorUnaryOp, ScalaPgaAlgebra}

object DefToString:
  def apply()(using algebra: ScalaPgaAlgebra): MultivectorUnaryOp =
    MultivectorUnaryOp { (cls, _) =>
      if (cls.variableFields.nonEmpty) {
        GeneratedCode { code =>
          code(
            s"""
               |override def toString: String =
               |  s"${cls.name}(${cls.variableFields.map(f => s"${f.name} = ${"$" + f.name}").mkString(", ")})"""".stripMargin)
        }
      } else if (cls == algebra.pointCenter) {
        GeneratedCode { code =>
          code(
            s"""
               |override def toString: String =
               |  "${cls.name}"""".stripMargin)
        }
      } else {
        throw new IllegalStateException(s"unknown class ${cls}")
      }
    }
