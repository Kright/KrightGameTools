package me.kright.gametools.pga.codegen.scala.common.ops

import me.kright.gametools.pga.codegen.scala.common.{GeneratedCode, MultivectorUnaryOp, ScalaPgaAlgebra}
import me.kright.gametools.symbolic.Sym

object DefMultiplyToScalar:
  def apply()(using algebra: ScalaPgaAlgebra): MultivectorUnaryOp = MultivectorUnaryOp { (cls, _) =>
    GeneratedCode { code =>
      val v = Sym("v")
      val result = cls.self * v
      val resultCls = algebra.findMatchingClass(result)
      code(
        s"""
           |@targetName("times")
           |def *(v: Double): ${resultCls.typeName} =""".stripMargin)
      code.block {
        code(resultCls.makeConstructor(result))
      }
    }
  }
