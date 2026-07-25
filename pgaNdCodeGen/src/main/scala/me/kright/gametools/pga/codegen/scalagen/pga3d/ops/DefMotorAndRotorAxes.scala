package me.kright.gametools.pga.codegen.scalagen.pga3d.ops

import me.kright.gametools.ga.PGA3
import me.kright.gametools.pga.codegen.common.AxesSymbolics
import me.kright.gametools.pga.codegen.scalagen.common.{GeneratedCode, MultivectorUnaryOp}
import me.kright.gametools.pga.codegen.scalagen.pga3d.Pga3dScalaAlgebra
import me.kright.gametools.pga.codegen.scalagen.pga3d.Pga3dScalaAlgebra.{motor, rotor, vector}

object DefMotorAndRotorAxes:
  def apply()(using pga3: PGA3): MultivectorUnaryOp = MultivectorUnaryOp { (cls, s) =>
    GeneratedCode { code =>
      if (cls == motor || cls == rotor) {
        for (axis <- AxesSymbolics.rotorAxes(cls.self, vector.self)) {
          val resultCls = Pga3dScalaAlgebra.findMatchingClass(axis.result)

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
