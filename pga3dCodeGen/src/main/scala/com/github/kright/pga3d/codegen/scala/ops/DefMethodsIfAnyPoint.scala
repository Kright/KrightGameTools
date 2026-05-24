package com.github.kright.pga3d.codegen.scala.ops

import me.kright.gametools.ga.{MultiVector, PGA3}
import com.github.kright.pga3d.codegen.scala.{GeneratedCode, ScalaMultivectorSubClass, MultivectorUnaryOp}
import com.github.kright.pga3d.codegen.scala.ScalaMultivectorSubClass.{point, projectivePoint, vector}
import com.github.kright.symbolic.Sym

object DefMethodsIfAnyPoint:
  def apply()(using pga3: PGA3): MultivectorUnaryOp =
    MultivectorUnaryOp { (cls, v) =>
      GeneratedCode { code =>
        val points = Set(projectivePoint, point, vector)
        if (points.contains(cls)) {
          if (cls == point) {
            code(
              s"""
                 |val center: ${point.typeName} =
                 |  ${point.typeName}(0.0, 0.0, 0.0)""".stripMargin)
          }

          code("")
          if (cls == projectivePoint) {
            val mv = MultiVector("wxy" -> Sym("wxy"), "wxz" -> Sym("wxz"), "wyz" -> Sym("wyz"), "xyz" -> Sym("xyz"))
            code(s"def blade3(wxy: Double, wxz: Double, wyz: Double, xyz: Double): ${cls.typeName} =")
            code.block {
              code(cls.makeConstructor(mv))
            }
          } else {
            val mv = MultiVector("wxy" -> Sym("wxy"), "wxz" -> Sym("wxz"), "wyz" -> Sym("wyz"))
            code(s"def blade3(wxy: Double, wxz: Double, wyz: Double): ${cls.typeName} =")
            code.block {
              code(cls.makeConstructor(mv))
            }
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
            code("")
            code(s"def mid(a: ${point.typeName}, b: ${point.typeName}): ${point.typeName} =")
            code.block {
              code(
                s"""${point.typeName}(
                   |  x = (a.x + b.x) * 0.5,
                   |  y = (a.y + b.y) * 0.5,
                   |  z = (a.z + b.z) * 0.5,
                   |)""".stripMargin)
            }

            code("")
            code(s"def mid(a: ${point.typeName}, b: ${point.typeName}, c: ${point.typeName}): ${point.typeName} =")
            code.block {
              code(
                s"""val m = 1.0 / 3.0
                   |${point.typeName}(
                   |  x = (a.x + b.x + c.x) * m,
                   |  y = (a.y + b.y + c.y) * m,
                   |  z = (a.z + b.z + c.z) * m,
                   |)""".stripMargin)
            }
          }
        }
      }
    }
