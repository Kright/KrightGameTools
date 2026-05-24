package me.kright.gametools.pga3d.codegen.scala.ops

import me.kright.gametools.ga.PGA3
import me.kright.gametools.pga3d.codegen.scala.{GeneratedCode, ScalaMultivectorSubClass, MultivectorUnaryOp}

object DefMotorToQuaternionAndTranslator {
  def apply()(using pga3: PGA3): MultivectorUnaryOp = MultivectorUnaryOp { (cls, v) =>
    GeneratedCode { code =>

      val self = cls.self
      
      if (cls == ScalaMultivectorSubClass.motor) {
        
        val quaternionTypeName = ScalaMultivectorSubClass.quaternion.typeName
        val translatorTypeName = ScalaMultivectorSubClass.translator.typeName
        
        code(
          s"""
             |/** motor has to be normalized */
             |def toQuaternionAndTranslator: ($quaternionTypeName, $translatorTypeName) = {
             |  val q = this.toQuaternionUnsafe
             |  val t = q.reverse.geometric(this)
             |  (q, t.toTranslatorUnsafe)
             |}
             |  
             |/** motor has to be normalized */
             |def toTranslatorAndQuaternion: ($translatorTypeName, $quaternionTypeName) = {
             |  val q = this.toQuaternionUnsafe
             |  val t = this.geometric(q.reverse)
             |  (t.toTranslatorUnsafe, q)
             |}""".stripMargin)
      }
    }
  }

}
