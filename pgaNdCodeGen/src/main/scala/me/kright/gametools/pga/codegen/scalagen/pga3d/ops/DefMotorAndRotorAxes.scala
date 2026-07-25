package me.kright.gametools.pga.codegen.scalagen.pga3d.ops

import me.kright.gametools.ga.PGA3
import me.kright.gametools.pga.codegen.scalagen.common.{GeneratedCode, MultivectorUnaryOp}
import me.kright.gametools.pga.codegen.scalagen.pga3d.Pga3dScalaAlgebra
import me.kright.gametools.pga.codegen.scalagen.pga3d.Pga3dScalaAlgebra.{motor, rotor, vector}
import me.kright.gametools.symbolic.Sym

object DefMotorAndRotorAxes:
  def apply()(using pga3: PGA3): MultivectorUnaryOp = MultivectorUnaryOp { (cls, s) =>
    GeneratedCode { code =>

      if (cls == motor || cls == rotor) {
        val self = cls.self
        val vec = vector.self
        val axes = vec.values.keys.toSeq.sortBy(_.bits).reverse.map(blade => vec.filter((b, _) => b == blade))

        for (axis <- axes) {
          val axisOne = axis.mapValues(_ => Sym(1.0))

          val isMinus = axis.values.values.head.toString.contains("-")
          val methodName = s"axis${axis.values.values.head.toString.replace("-", "").toUpperCase}"

          val result = if (isMinus) self.sandwich(axisOne) * Sym(-1.0) else self.sandwich(axisOne)
          val resultCls = Pga3dScalaAlgebra.findMatchingClass(result)

          code(
            s"""
               |def $methodName: ${resultCls.typeName} =""".stripMargin
          )
          code.block {
            if (cls == rotor) {
              code(resultCls.makeConstructor(result))
            } else {
              code(s"toRotorUnsafe.$methodName")
            }
          }
        }
      }
    }
  }
