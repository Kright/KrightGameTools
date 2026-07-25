package me.kright.gametools.pga.codegen.scalagen.pga2d.ops

import me.kright.gametools.ga.PGA2
import me.kright.gametools.pga.codegen.scalagen.common.{GeneratedCode, MultivectorUnaryOp}
import me.kright.gametools.pga.codegen.scalagen.pga2d.Pga2dScalaAlgebra

object DefProjection:
  def apply()(using pga2: PGA2): MultivectorUnaryOp =
    val pointClasses = Set(
      Pga2dScalaAlgebra.projectivePoint,
      Pga2dScalaAlgebra.point,
      Pga2dScalaAlgebra.pointCenter
    )

    val lineClass = Pga2dScalaAlgebra.line

    MultivectorUnaryOp { (cls, v) =>
      if (pointClasses.contains(cls)) {
        GeneratedCode { code =>
          val point = cls.self
          val line = lineClass.makeSymbolic("line")
          val result = line.dot(point).geometric(line)
          val resultCls = Pga2dScalaAlgebra.findMatchingClass(result)

          code(
            s"""
               |/** fused line.dot(point).geometric(line) */
               |def projectOntoLine(line: ${lineClass.typeName}): ${resultCls.typeName} =""".stripMargin)
          code.block {
            code(resultCls.makeConstructorOptimized(result, resultCls))
          }
        }
      } else None
    }
