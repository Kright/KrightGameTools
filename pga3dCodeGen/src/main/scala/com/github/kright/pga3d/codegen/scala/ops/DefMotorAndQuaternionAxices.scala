package com.github.kright.pga3d.codegen.scala.ops

import me.kright.gametools.ga.PGA3
import com.github.kright.pga3d.codegen.scala.{GeneratedCode, ScalaMultivectorSubClass, MultivectorUnaryOp}
import com.github.kright.symbolic.Sym

object DefMotorAndQuaternionAxices:
  def apply()(using pga3: PGA3): MultivectorUnaryOp = MultivectorUnaryOp { (cls, s) =>
    GeneratedCode { code =>

      if (cls == ScalaMultivectorSubClass.motor || cls == ScalaMultivectorSubClass.quaternion) {
        val self = cls.self
        val vec = ScalaMultivectorSubClass.vector.self
        val axes = vec.values.keys.toSeq.sortBy(_.bits).reverse.map(blade => vec.filter((b, _) => b == blade))

        for (axe <- axes) {
          val axeOne = axe.mapValues(_ => Sym(1.0))
          
          val isMinus = axe.values.values.head.toString.contains("-")
          val methodName = s"axis${axe.values.values.head.toString.replace("-", "").toUpperCase}"

          val result = if (isMinus) self.sandwich(axeOne) * Sym(-1.0) else self.sandwich(axeOne)
          val resultCls = ScalaMultivectorSubClass.findMatchingClass(result)
          
          code(
            s"""
               |def $methodName: ${resultCls.typeName} =""".stripMargin
          )
          code.block {
            if (cls == ScalaMultivectorSubClass.quaternion) {
              code(resultCls.makeConstructor(result))
            } else {
              code(s"toQuaternionUnsafe.$methodName")
            }
          }
        }
      }
    }
  }
