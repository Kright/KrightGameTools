package com.github.kright.pga3d.codegen.scala.ops

import me.kright.gametools.ga.PGA3
import com.github.kright.pga3d.codegen.scala.{GeneratedCode, MultivectorUnaryOp, ScalaMultivectorSubClass}
import com.github.kright.symbolic.Sym
import com.github.kright.symbolic.Sym.given_Numeric_Sym.mkNumericOps

object DefConvertTo:
  def apply()(using pga3: PGA3): MultivectorUnaryOp =
    MultivectorUnaryOp { (cls, v) =>
      GeneratedCode { code =>
        for (target <- ScalaMultivectorSubClass.pgaClasses if target != cls) {
          if (target.isMatching(cls.self)) {
            code(s"\ndef to${target.typeNameWithoutPrefix}: ${target.typeName} =")
            code.block {
              code(target.makeConstructor(cls.self))
            }
          } else if (target != ScalaMultivectorSubClass.scalar && target != ScalaMultivectorSubClass.pseudoScalar) {
            val simplifiedSelf = cls.self.filter((blade, _) => target.variableFields.exists(_.basisBlade == blade))
            if (simplifiedSelf.values.nonEmpty) {
              code(s"\ndef to${target.typeNameWithoutPrefix}Unsafe: ${target.typeName} =")
              code.block {
                code(target.makeConstructor(cls.self))
              }
            }
          }
        }

        if (cls == ScalaMultivectorSubClass.projectivePoint) {
          val target = ScalaMultivectorSubClass.point
          val xyzBlade = pga3.representation.basisBladeWithSign("xyz").basisBlade

          code(s"\ndef to${target.typeNameWithoutPrefix}: ${target.typeName} =")
          code.block {
            val xyz = cls.self("xyz")
            val r = cls.self.filter((b, _) => b != xyzBlade).map((_, s) => s * Sym("mult"))
            code(s"val mult = ${Sym(1.0) / xyz}")
            code(target.makeConstructor(r))
          }
        }

        if (cls == ScalaMultivectorSubClass.projectiveTranslator) {
          val target = ScalaMultivectorSubClass.translator

          code(s"\ndef to${target.typeNameWithoutPrefix}: ${target.typeName} =")
          code.block {
            val mult = Sym(1.0) / cls.self("s")
            val result = cls.self.filter((b, _) => b.grade == 2).map((_, s) => s * Sym("mult"))
            code(s"val mult = ${Sym(1.0) / cls.self("s")}")
            code(target.makeConstructor(result))
          }
        }
      }
    }
