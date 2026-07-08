package me.kright.gametools.pga.codegen.scala.common.ops

import me.kright.gametools.pga.codegen.scala.common.{GeneratedCode, MultivectorUnaryOp, ScalaPgaAlgebra}

object DefDistanceToPoint:
  def apply()(using algebra: ScalaPgaAlgebra): MultivectorUnaryOp =
    val pointClass = algebra.point

    MultivectorUnaryOp { (cls, _) =>
      if (cls == pointClass) {
        GeneratedCode { code =>
          code(
            s"""
               |def distanceTo(point: ${pointClass.typeName}): Double =
               |  (this - point).norm""".stripMargin)
        }
      } else None
    }
