package me.kright.gametools.pga3d.codegen.scala.ops

import me.kright.gametools.ga.PGA3
import me.kright.gametools.pga3d.codegen.scala.{GeneratedCode, ScalaMultivectorSubClass, MultivectorUnaryOp}
import me.kright.gametools.symbolic.Sym

object DefProjection:
  def apply()(using pga3: PGA3): MultivectorUnaryOp =
    val pointClasses = Set(
      ScalaMultivectorSubClass.projectivePoint,
      ScalaMultivectorSubClass.point,
      ScalaMultivectorSubClass.pointCenter
    )

    val lineClass = ScalaMultivectorSubClass.bivector
    val planeClass = ScalaMultivectorSubClass.plane

    MultivectorUnaryOp { (cls, v) =>
      if (cls == lineClass) {
        GeneratedCode { code =>
          val line = cls.self

          for (planeClass <- Seq(ScalaMultivectorSubClass.plane, ScalaMultivectorSubClass.planeIdeal)) {
            val plane = planeClass.makeSymbolic("plane")
            val result = -plane.dot(line).geometric(plane)
            val resultCls = ScalaMultivectorSubClass.findMatchingClass(result)

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
            val resultCls = ScalaMultivectorSubClass.findMatchingClass(result)

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
            val tunedResultCls = ScalaMultivectorSubClass.findMatchingClass(tunedResult)

            code(
              """
                |/**
                | * fused -line.dot(point).geometric(line).toPointUnsafe
                | * not applicable for Bivector, input should be a line
                | * example of result for Bivector:
                |""".stripMargin
            )
            code(ScalaMultivectorSubClass.findMatchingClass(result).makeConstructor(result).split("\n").map(s => s" * $s").mkString("\n"))
            code(" */")
            code(s"def projectOntoLine(line: ${lineClass.typeName}): ${tunedResultCls.typeName} =")
            code.block {
              code(tunedResultCls.makeConstructorOptimized(tunedResult, tunedResultCls))
            }
          }
        }
      } else None
    }