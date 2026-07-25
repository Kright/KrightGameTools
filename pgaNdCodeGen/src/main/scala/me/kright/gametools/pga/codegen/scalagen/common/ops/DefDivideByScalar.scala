package me.kright.gametools.pga.codegen.scalagen.common.ops

import me.kright.gametools.pga.codegen.scalagen.common.{GeneratedCode, MultivectorUnaryOp, ScalaPgaAlgebra}
import me.kright.gametools.symbolic.Sym

object DefDivideByScalar:
  def apply()(using algebra: ScalaPgaAlgebra): MultivectorUnaryOp = MultivectorUnaryOp { (cls, _) =>
    GeneratedCode { code =>
      val r = Sym("r")
      val result = cls.self * r
      val resultCls = algebra.findMatchingClass(result)
      code(
        s"""
           |/** multiplies by the reciprocal: one division instead of one per component, at the cost of one extra rounding (~0.5 ulp) */
           |@targetName("div")
           |def /(r: Double): ${resultCls.typeName} =""".stripMargin)
      code.block {
        code("this * (1.0 / r)")
      }
    }
  }
