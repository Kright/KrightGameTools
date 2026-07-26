package me.kright.gametools.pga.codegen.scalagen.pga3d.ops

import me.kright.gametools.ga.PGA3
import me.kright.gametools.pga.codegen.common.{FormulaTemplate, SharedFormulas}
import me.kright.gametools.pga.codegen.scalagen.common.{GeneratedCode, MultivectorUnaryOp}
import me.kright.gametools.pga.codegen.scalagen.pga3d.Pga3dScalaAlgebra
import me.kright.gametools.pga.codegen.scalagen.pga3d.Pga3dScalaAlgebra.{bivector, bivectorBulk, bivectorWeight, motor, rotor, translator}

object DefLogForMotor:
  def apply()(using pga3: PGA3): MultivectorUnaryOp = MultivectorUnaryOp { (cls, v) =>
    GeneratedCode { code =>
      val self = cls.self
      val prefix = Pga3dScalaAlgebra.typeNamePrefix

      if (cls == motor) {
        code(s"\ndef log: ${bivector.typeName} =")
        code.block {
          code(FormulaTemplate.renderScala(SharedFormulas.motorLog, prefix))
          code("")
          code(bivector.makeConstructor(SharedFormulas.motorLogResult(self)))
          code("")
        }
        code(s"\n/** the fractional power of the motion: pow(0.5) is the half motion, pow(2.0) applies it twice */")
        code(s"def pow(t: Double): ${cls.name} =")
        code.block {
          code("log.exp(t)")
        }
      }
      if (cls == translator) {
        code(s"\ndef log: ${bivectorWeight.typeName} =")
        code.block {
          code(bivectorWeight.makeConstructor(self.weight))
        }
        code(s"\n/** the fractional power of the motion: pow(0.5) is the half motion, pow(2.0) applies it twice */")
        code(s"def pow(t: Double): ${cls.name} =")
        code.block {
          code("log.exp(t)")
        }
      }
      if (cls == rotor) {
        code(s"\ndef log: ${bivectorBulk.typeName} =")
        code.block {
          code(FormulaTemplate.renderScala(SharedFormulas.rotorLog, prefix))
          code("")
          code(bivectorBulk.makeConstructor(SharedFormulas.rotorLogResult(self)))
          code("")
        }
        code(s"\n/** the fractional power of the motion: pow(0.5) is the half motion, pow(2.0) applies it twice */")
        code(s"def pow(t: Double): ${cls.name} =")
        code.block {
          code("log.exp(t)")
        }
      }
    }
  }
