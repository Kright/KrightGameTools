package me.kright.gametools.pga.codegen.scala.common.ops

import me.kright.gametools.pga.codegen.scala.common.{GeneratedCode, MultivectorUnaryOp, ScalaPgaAlgebra}

object DefMotorToRotorAndTranslator {
  def apply()(using algebra: ScalaPgaAlgebra): MultivectorUnaryOp = MultivectorUnaryOp { (cls, _) =>
    GeneratedCode { code =>

      val self = cls.self

      if (cls == algebra.motor) {

        val rotorTypeName = algebra.rotor.typeName
        val translatorTypeName = algebra.translator.typeName

        code(
          s"""
             |/** motor has to be normalized */
             |def toRotorAndTranslator: ($rotorTypeName, $translatorTypeName) = {
             |  val q = this.toRotorUnsafe
             |  val t = q.reverse.geometric(this)
             |  (q, t.toTranslatorUnsafe)
             |}
             |
             |/** motor has to be normalized */
             |def toTranslatorAndRotor: ($translatorTypeName, $rotorTypeName) = {
             |  val q = this.toRotorUnsafe
             |  val t = this.geometric(q.reverse)
             |  (t.toTranslatorUnsafe, q)
             |}""".stripMargin)
      }
    }
  }

}
