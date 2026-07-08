package me.kright.gametools.pga.codegen.scala.common.ops

import me.kright.gametools.pga.codegen.scala.common.{GeneratedCode, MultivectorUnaryOp, ScalaPgaAlgebra}

object DefVariablesComponentsCount:
  def apply()(using algebra: ScalaPgaAlgebra): MultivectorUnaryOp =
    MultivectorUnaryOp { (cls, _) =>
      GeneratedCode { code =>
        code("")
        code(s"inline val componentsCount = ${cls.variableFields.size}")
      }
    }
