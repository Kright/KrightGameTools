package me.kright.gametools.pga.codegen.scala.common.ops

import me.kright.gametools.ga.MultiVector
import me.kright.gametools.pga.codegen.scala.common.{GeneratedCode, MultivectorUnaryOp, ScalaPgaAlgebra}
import me.kright.gametools.symbolic.Sym

object DefMethodsIfAnyPoint:
  def apply()(using algebra: ScalaPgaAlgebra): MultivectorUnaryOp =
    import algebra.given

    MultivectorUnaryOp { (cls, _) =>
      GeneratedCode { code =>
        val point = algebra.point
        val projectivePoint = algebra.projectivePoint
        val vector = algebra.vector
        val points = Set(projectivePoint, point, vector)

        if (points.contains(cls)) {
          if (cls == point) {
            code(
              s"""
                 |val center: ${point.typeName} =
                 |  ${point.typeName}(${point.variableFields.map(_ => "0.0").mkString(", ")})""".stripMargin)
          }

          code("")
          // the top-grade blade of projectivePoint (e.g. grade 3 -> "blade3", fields wxy/wxz/wyz/xyz;
          // grade 2 -> "blade2", fields wx/wy/xy), alphabetically sorted to match the generated order
          val topGrade = projectivePoint.variableFields.head.basisBlade.grade
          val bladeFields = if (cls == projectivePoint) projectivePoint.variableFields else cls.variableFields
          val bladeNames = bladeFields.map(f => algebra.pga.representation(f.basisBlade)).sorted
          val bladeValue = MultiVector[Sym](bladeNames.map(n => n -> Sym(n))*)

          code(s"def blade$topGrade(${bladeNames.map(n => s"$n: Double").mkString(", ")}): ${cls.typeName} =")
          code.block {
            code(cls.makeConstructor(bladeValue))
          }

          code("")
          code(s"def interpolate(a: ${cls.typeName}, b: ${cls.typeName}, t: Double): ${cls.typeName} =")
          code.block {
            if (cls == projectivePoint || cls == vector) {
              code("a * (1.0 - t) + b * t")
            } else {
              code("(a.toVectorUnsafe * (1.0 - t) + b.toVectorUnsafe * t).toPointUnsafe")
            }
          }

          if (cls == point) {
            val names = point.variableFields.map(_.name)

            code("")
            code(s"def mid(a: ${point.typeName}, b: ${point.typeName}): ${point.typeName} =")
            code.block {
              val lines = names.map(n => s"  $n = (a.$n + b.$n) * 0.5,").mkString("\n")
              code(s"${point.typeName}(\n$lines\n)")
            }

            code("")
            code(s"def mid(a: ${point.typeName}, b: ${point.typeName}, c: ${point.typeName}): ${point.typeName} =")
            code.block {
              val lines = names.map(n => s"  $n = (a.$n + b.$n + c.$n) * m,").mkString("\n")
              code(s"val m = 1.0 / 3.0\n${point.typeName}(\n$lines\n)")
            }
          }
        }
      }
    }
