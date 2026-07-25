package me.kright.gametools.pga.codegen.scalagen.common.ops

import me.kright.gametools.pga.codegen.scalagen.common.{GeneratedCode, MultivectorUnaryOp, ScalaPgaAlgebra}
import me.kright.gametools.symbolic.Sym

object DefDivideByScalar:
  def apply()(using algebra: ScalaPgaAlgebra): MultivectorUnaryOp = MultivectorUnaryOp { (cls, _) =>
    GeneratedCode { code =>
      val v = Sym("v")
      val result = cls.self * v
      val resultCls = algebra.findMatchingClass(result)
      code(
        s"""
           |/** multiplies by the reciprocal: one division instead of one per component, at the cost of one extra rounding (~0.5 ulp) */
           |@targetName("div")
           |def /(v: Double): ${resultCls.typeName} =""".stripMargin)
      code.block {
        code("this * (1.0 / v)")
      }
    }
  }
