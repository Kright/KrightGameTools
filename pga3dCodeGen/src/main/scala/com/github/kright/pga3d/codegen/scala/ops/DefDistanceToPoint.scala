package com.github.kright.pga3d.codegen.scala.ops

import me.kright.gametools.ga.PGA3
import com.github.kright.pga3d.codegen.scala.{GeneratedCode, ScalaMultivectorSubClass, MultivectorUnaryOp}

object DefDistanceToPoint:
  def apply()(using pga3: PGA3): MultivectorUnaryOp =
    val pointClass = ScalaMultivectorSubClass.point

    MultivectorUnaryOp { (cls, v) =>
      if (cls == pointClass) {
        GeneratedCode { code =>
          code(
            s"""
               |def distanceTo(point: ${pointClass.typeName}): Double =
               |  (this - point).norm""".stripMargin)
        }
      } else None
    }