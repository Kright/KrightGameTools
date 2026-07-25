package me.kright.gametools.pga.codegen.scalagen.pga3d.ops

import me.kright.gametools.ga.PGA3
import me.kright.gametools.pga.codegen.scalagen.common.{GeneratedCode, MultivectorUnaryOp}
import me.kright.gametools.pga.codegen.scalagen.pga3d.Pga3dScalaAlgebra
import scala.collection.immutable.ArraySeq

object DefProjection:
  def apply()(using pga3: PGA3): MultivectorUnaryOp =
    val pointClasses = Set(
      Pga3dScalaAlgebra.projectivePoint,
      Pga3dScalaAlgebra.point,
      Pga3dScalaAlgebra.pointCenter
    )

    val lineClass = Pga3dScalaAlgebra.bivector

    MultivectorUnaryOp { (cls, v) =>
      if (cls == lineClass) {
        GeneratedCode { code =>
          val line = cls.self

          for (hyperplaneClass <- ArraySeq(Pga3dScalaAlgebra.plane, Pga3dScalaAlgebra.planeCentral)) {
            val plane = hyperplaneClass.makeSymbolic("plane")
            val result = -plane.dot(line).geometric(plane)
            val resultCls = Pga3dScalaAlgebra.findMatchingClass(result)

            code(
              s"""
                 |/** fused -plane.dot(line).geometric(plane); the result is the line scaled by plane.normSquare > 0 */
                 |def projectOntoPlane(plane: ${hyperplaneClass.typeName}): ${resultCls.typeName} =""".stripMargin)
            code.block {
              code(resultCls.makeConstructorOptimized(result, resultCls))
            }
          }
        }
      } else if (pointClasses.contains(cls)) {
        GeneratedCode { code =>
          val point = cls.self

          for (hyperplaneClass <- ArraySeq(Pga3dScalaAlgebra.plane, Pga3dScalaAlgebra.planeCentral)) {
            val plane = hyperplaneClass.makeSymbolic("plane")
            val result = plane.dot(point).geometric(plane)
            val resultCls = Pga3dScalaAlgebra.findMatchingClass(result)

            code(
              s"""
                 |/** fused plane.dot(point).geometric(plane); w of the result is plane.normSquare > 0, so w = 1 for a normalized plane */
                 |def projectOntoPlane(plane: ${hyperplaneClass.typeName}): ${resultCls.typeName} =""".stripMargin)
            code.block {
              code(resultCls.makeConstructorOptimized(result, resultCls))
            }
          }

          {
            val line = lineClass.makeSymbolic("line")
            val result = -line.dot(point).geometric(line)
            val tunedResult = result.filter((b, _) => b.grade != 1)
            val tunedResultCls = Pga3dScalaAlgebra.findMatchingClass(tunedResult)

            code(
              """
                |/**
                | * fused -line.dot(point).geometric(line).toPointUnsafe
                | * not applicable for Bivector, input should be a line
                | * example of result for Bivector:
                |""".stripMargin
            )
            code(Pga3dScalaAlgebra.findMatchingClass(result).makeConstructor(result).split("\n").map(s => s" * $s").mkString("\n"))
            code(" */")
            code(s"def projectOntoLine(line: ${lineClass.typeName}): ${tunedResultCls.typeName} =")
            code.block {
              code(tunedResultCls.makeConstructorOptimized(tunedResult, tunedResultCls))
            }
          }
        }
      } else None
    }
