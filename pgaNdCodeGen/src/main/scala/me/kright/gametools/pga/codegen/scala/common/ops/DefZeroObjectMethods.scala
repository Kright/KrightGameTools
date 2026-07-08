package me.kright.gametools.pga.codegen.scala.common.ops

import me.kright.gametools.pga.codegen.scala.common.{GeneratedCode, MultivectorUnaryOp, ScalaPgaAlgebra}

object DefZeroObjectMethods:
  def apply()(using algebra: ScalaPgaAlgebra): MultivectorUnaryOp =
    MultivectorUnaryOp { (cls, _) =>
      GeneratedCode { code =>
        if (cls != algebra.point && cls != algebra.rotor && cls != algebra.translator) {
          code("")
          code(s"val zero: ${cls.typeName} = ${cls.typeName}(${cls.variableFields.map(_ => "0.0").mkString(", ")})")
        }
      }
    }
