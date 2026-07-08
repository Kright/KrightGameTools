package me.kright.gametools.pga.codegen.scala.pga3d.ops

import me.kright.gametools.ga.PGA3
import me.kright.gametools.pga.codegen.scala.common.{GeneratedCode, MultivectorUnaryOp}
import me.kright.gametools.pga.codegen.scala.pga3d.Pga3dScalaAlgebra

object DefProjection:
  def apply()(using pga3: PGA3): MultivectorUnaryOp =
    val pointClasses = Set(
      Pga3dScalaAlgebra.projectivePoint,
      Pga3dScalaAlgebra.point,
      Pga3dScalaAlgebra.pointCenter
    )

    val lineClass = Pga3dScalaAlgebra.bivector
    val planeClass = Pga3dScalaAlgebra.plane

    MultivectorUnaryOp { (cls, v) =>
      if (cls == lineClass) {
        GeneratedCode { code =>
          val line = cls.self

          for (planeClass <- Seq(Pga3dScalaAlgebra.plane, Pga3dScalaAlgebra.planeIdeal)) {
            val plane = planeClass.makeSymbolic("plane")
            val result = -plane.dot(line).geometric(plane)
            val resultCls = Pga3dScalaAlgebra.findMatchingClass(result)

            code(
              s"""
                 |/** fused plane.dot(line).geometric(plane) */
                 |def projectOntoPlane(plane: ${planeClass.typeName}): ${resultCls.typeName} =""".stripMargin)
            code.block {
              code(resultCls.makeConstructorOptimized(result, resultCls))
            }
          }
        }
      } else if (pointClasses.contains(cls)) {
        GeneratedCode { code =>
          val point = cls.self

          {
            val plane = planeClass.makeSymbolic("plane")
            val result = plane.dot(point).geometric(plane)
            val resultCls = Pga3dScalaAlgebra.findMatchingClass(result)

            code(
              s"""
                 |/** fused plane.dot(point).geometric(plane) */
                 |def projectOntoPlane(plane: ${planeClass.typeName}): ${resultCls.typeName} =""".stripMargin)
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
