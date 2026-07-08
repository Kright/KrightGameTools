package me.kright.gametools.pga.codegen.scala.common.ops

import me.kright.gametools.pga.codegen.scala.common.{GeneratedCode, MultivectorUnaryOp, ScalaPgaAlgebra}
import me.kright.gametools.symbolic.Sym
import me.kright.gametools.symbolic.Sym.given_Numeric_Sym.mkNumericOps

object DefConvertTo:
  def apply()(using algebra: ScalaPgaAlgebra): MultivectorUnaryOp =
    MultivectorUnaryOp { (cls, _) =>
      GeneratedCode { code =>
        for (target <- algebra.pgaClasses if target != cls) {
          if (target.isMatching(cls.self)) {
            code(s"\ndef to${target.typeNameWithoutPrefix}: ${target.typeName} =")
            code.block {
              code(target.makeConstructor(cls.self))
            }
          } else if (target != algebra.scalar && target != algebra.pseudoScalar) {
            val simplifiedSelf = cls.self.filter((blade, _) => target.variableFields.exists(_.basisBlade == blade))
            if (simplifiedSelf.values.nonEmpty) {
              code(s"\ndef to${target.typeNameWithoutPrefix}Unsafe: ${target.typeName} =")
              code.block {
                code(target.makeConstructor(cls.self))
              }
            }
          }
        }

        if (cls == algebra.projectivePoint) {
          val target = algebra.point
          // the sole bulk field of projectivePoint (the one not containing the ideal generator), e.g. xyz / xy
          val bulkField = target.constantFields.head._1

          code(s"\ndef to${target.typeNameWithoutPrefix}: ${target.typeName} =")
          code.block {
            val bulk = cls.self(bulkField.basisBladeWithSign)
            val r = cls.self.filter((b, _) => b != bulkField.basisBlade).map((_, s) => s * Sym("mult"))
            code(s"val mult = ${Sym(1.0) / bulk}")
            code(target.makeConstructor(r))
          }
        }

        if (cls == algebra.projectiveTranslator) {
          val target = algebra.translator

          code(s"\ndef to${target.typeNameWithoutPrefix}: ${target.typeName} =")
          code.block {
            val result = cls.self.filter((b, _) => b.grade == 2).map((_, s) => s * Sym("mult"))
            code(s"val mult = ${Sym(1.0) / cls.self("s")}")
            code(target.makeConstructor(result))
          }
        }
      }
    }
