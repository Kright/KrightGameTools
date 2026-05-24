package com.github.kright.pga3d.codegen.scala.ops

import me.kright.gametools.ga.{MultiVector, PGA3}
import com.github.kright.pga3d.codegen.scala.{GeneratedCode, ScalaMultivectorSubClass, MultivectorUnaryOp}
import com.github.kright.symbolic.Sym

object DefMultiplyToScalar:
  def apply()(using pga3: PGA3): MultivectorUnaryOp = MultivectorUnaryOp { (cls, s) =>
    GeneratedCode { code =>
      val v = Sym("v")
      val result = cls.self * v
      val resultCls = ScalaMultivectorSubClass.findMatchingClass(result)
      code(
        s"""
           |@targetName("times")
           |def *(v: Double): ${resultCls.typeName} =""".stripMargin)
      code.block {
        code(resultCls.makeConstructor(result))
      }
    }
  }
