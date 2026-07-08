package me.kright.gametools.pga.codegen.scala.common.ops

import me.kright.gametools.ga.MultiVector
import me.kright.gametools.pga.codegen.scala.common.{GeneratedCode, MultivectorUnaryOp, ScalaPgaAlgebra}
import me.kright.gametools.symbolic.Sym

object DefObjectMethodsForTranslator:
  def apply()(using algebra: ScalaPgaAlgebra): MultivectorUnaryOp =
    import algebra.given

    MultivectorUnaryOp { (cls, _) =>
      GeneratedCode { code =>
        if (cls == algebra.translator) {
          val idArgs = cls.variableFields.map(_ => "0.0").mkString(", ")
          code(
            s"""
               |val id: ${cls.typeName} = ${cls.typeName}($idArgs)""".stripMargin)
          code("")
          code(s"def addVector(v: ${algebra.vector.typeName}): ${cls.typeName} =")
          code.block {
            val vv = algebra.vector.makeSymbolic("v")
            val mult = MultiVector("w" -> Sym(-0.5))
            code(cls.makeConstructor(mult.geometric(vv.dual)))
          }
        }
      }
    }
