package me.kright.gametools.pga.codegen.scalagen.pga2d.ops

import me.kright.gametools.ga.PGA2
import me.kright.gametools.pga.codegen.common.AxesSymbolics
import me.kright.gametools.pga.codegen.scalagen.common.{GeneratedCode, MultivectorUnaryOp}
import me.kright.gametools.pga.codegen.scalagen.pga2d.Pga2dScalaAlgebra
import me.kright.gametools.pga.codegen.scalagen.pga2d.Pga2dScalaAlgebra.{motor, rotor, vector}

object DefMotorAndRotorAxes:
  def apply()(using pga2: PGA2): MultivectorUnaryOp = MultivectorUnaryOp { (cls, s) =>
    GeneratedCode { code =>
      if (cls == motor || cls == rotor) {
        for (axis <- AxesSymbolics.rotorAxes(cls.self, vector.self)) {
          val resultCls = Pga2dScalaAlgebra.findMatchingClass(axis.result)

          code(
            s"""
               |def ${axis.methodName}: ${resultCls.typeName} =""".stripMargin
          )
          code.block {
            if (cls == rotor) {
              code(resultCls.makeConstructor(axis.result))
            } else {
              code(s"toRotorUnsafe.${axis.methodName}")
            }
          }
        }
      }
    }
  }
