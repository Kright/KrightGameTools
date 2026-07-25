package me.kright.gametools.pga.codegen.scalagen.pga2d.ops

import me.kright.gametools.ga.PGA2
import me.kright.gametools.pga.codegen.scalagen.common.{GeneratedCode, MultivectorUnaryOp}
import me.kright.gametools.pga.codegen.scalagen.pga2d.Pga2dScalaAlgebra
import scala.collection.immutable.ArraySeq

object DefProjection:
  def apply()(using pga2: PGA2): MultivectorUnaryOp =
    val pointClasses = Set(
      Pga2dScalaAlgebra.projectivePoint,
      Pga2dScalaAlgebra.point,
      Pga2dScalaAlgebra.pointCenter
    )

    MultivectorUnaryOp { (cls, v) =>
      if (pointClasses.contains(cls)) {
        GeneratedCode { code =>
          val point = cls.self

          for (hyperplaneClass <- ArraySeq(Pga2dScalaAlgebra.line, Pga2dScalaAlgebra.lineCentral)) {
            val line = hyperplaneClass.makeSymbolic("line")
            val result = -line.dot(point).geometric(line)
            val resultCls = Pga2dScalaAlgebra.findMatchingClass(result)

            code(
              s"""
                 |/** fused -line.dot(point).geometric(line); w of the result is line.normSquare > 0, so w = 1 for a normalized line */
                 |def projectOntoLine(line: ${hyperplaneClass.typeName}): ${resultCls.typeName} =""".stripMargin)
            code.block {
              code(resultCls.makeConstructorOptimized(result, resultCls))
            }
          }
        }
      } else None
    }
