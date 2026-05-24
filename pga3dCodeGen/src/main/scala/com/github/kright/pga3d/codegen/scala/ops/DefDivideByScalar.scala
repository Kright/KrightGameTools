package com.github.kright.pga3d.codegen.scala.ops

import me.kright.gametools.ga.PGA3
import com.github.kright.pga3d.codegen.scala.{GeneratedCode, ScalaMultivectorSubClass, MultivectorUnaryOp}
import com.github.kright.symbolic.Sym

object DefDivideByScalar:
  def apply()(using pga3: PGA3): MultivectorUnaryOp = MultivectorUnaryOp { (cls, s) =>
    GeneratedCode { code =>
      val v = Sym("v")
      val result = cls.self * v
      val resultCls = ScalaMultivectorSubClass.findMatchingClass(result)
      code(
        s"""
           |@targetName("div")
           |def /(v: Double): ${resultCls.typeName} =""".stripMargin)
      code.block {
        code("this * (1.0 / v)")
      }
    }
  }
