package me.kright.gametools.pga3d.codegen.scala.ops

import me.kright.gametools.ga.PGA3
import me.kright.gametools.pga3d.codegen.scala.ScalaMultivectorSubClass.{motor, translator, vector}
import me.kright.gametools.pga3d.codegen.scala.{GeneratedCode, MultivectorUnaryOp}

object DefObjectMethodsForMotor:
  def apply()(using pga3: PGA3): MultivectorUnaryOp =
    MultivectorUnaryOp { (cls, v) =>
      GeneratedCode { code =>
        if (cls == motor) {
          code(
            s"""
               |val id: ${cls.typeName} = ${cls.typeName}(1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
               |
               |def addVector(v: ${vector.typeName}): ${cls.typeName} = ${translator.name}.addVector(v).toMotor""".stripMargin)
        }
      }
    }
