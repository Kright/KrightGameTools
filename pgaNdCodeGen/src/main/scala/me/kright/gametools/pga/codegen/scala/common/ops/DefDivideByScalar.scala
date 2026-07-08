package me.kright.gametools.pga.codegen.scala.common.ops

import me.kright.gametools.pga.codegen.scala.common.{GeneratedCode, MultivectorUnaryOp, ScalaPgaAlgebra}
import me.kright.gametools.symbolic.Sym

object DefDivideByScalar:
  def apply()(using algebra: ScalaPgaAlgebra): MultivectorUnaryOp = MultivectorUnaryOp { (cls, _) =>
    GeneratedCode { code =>
      val v = Sym("v")
      val result = cls.self * v
      val resultCls = algebra.findMatchingClass(result)
      code(
        s"""
           |@targetName("div")
           |def /(v: Double): ${resultCls.typeName} =""".stripMargin)
      code.block {
        code("this * (1.0 / v)")
      }
    }
  }
