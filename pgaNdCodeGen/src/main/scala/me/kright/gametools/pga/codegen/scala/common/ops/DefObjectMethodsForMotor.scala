package me.kright.gametools.pga.codegen.scala.common.ops

import me.kright.gametools.pga.codegen.scala.common.{GeneratedCode, MultivectorUnaryOp, ScalaPgaAlgebra}

object DefObjectMethodsForMotor:
  def apply()(using algebra: ScalaPgaAlgebra): MultivectorUnaryOp =
    MultivectorUnaryOp { (cls, _) =>
      GeneratedCode { code =>
        if (cls == algebra.motor) {
          val idArgs = cls.variableFields.zipWithIndex.map((_, i) => if (i == 0) "1.0" else "0.0").mkString(", ")
          code(
            s"""
               |val id: ${cls.typeName} = ${cls.typeName}($idArgs)
               |
               |def addVector(v: ${algebra.vector.typeName}): ${cls.typeName} = ${algebra.translator.name}.addVector(v).toMotor""".stripMargin)
        }
      }
    }
