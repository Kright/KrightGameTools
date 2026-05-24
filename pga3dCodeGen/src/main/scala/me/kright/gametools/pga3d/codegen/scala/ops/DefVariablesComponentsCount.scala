package me.kright.gametools.pga3d.codegen.scala.ops

import me.kright.gametools.ga.PGA3
import me.kright.gametools.pga3d.codegen.scala.{GeneratedCode, MultivectorUnaryOp}

object DefVariablesComponentsCount:
  def apply()(using pga3: PGA3): MultivectorUnaryOp =
    MultivectorUnaryOp { (cls, v) =>
      GeneratedCode { code =>
        code("")
        code(s"inline val componentsCount = ${cls.variableFields.size}")
      }
    }
