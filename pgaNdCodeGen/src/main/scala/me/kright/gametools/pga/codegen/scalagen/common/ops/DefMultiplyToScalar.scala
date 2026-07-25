package me.kright.gametools.pga.codegen.scalagen.common.ops

import me.kright.gametools.pga.codegen.scalagen.common.{GeneratedCode, MultivectorUnaryOp, ScalaPgaAlgebra}
import me.kright.gametools.symbolic.Sym

object DefMultiplyToScalar:
  def apply()(using algebra: ScalaPgaAlgebra): MultivectorUnaryOp = MultivectorUnaryOp { (cls, _) =>
    GeneratedCode { code =>
      val r = Sym("r")
      val result = cls.self * r
      val resultCls = algebra.findMatchingClass(result)
      code(
        s"""
           |@targetName("times")
           |def *(r: Double): ${resultCls.typeName} =""".stripMargin)
      code.block {
        code(resultCls.makeConstructor(result))
      }
    }
  }
