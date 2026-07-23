package me.kright.gametools.pga.codegen.scala.common.ops

import me.kright.gametools.ga.MultiVector
import me.kright.gametools.pga.codegen.scala.common.{GeneratedCode, MultivectorUnaryOp, ScalaPgaAlgebra}
import me.kright.gametools.symbolic.Sym

object DefPlusMinusMadd:
  def apply()(using algebra: ScalaPgaAlgebra): MultivectorUnaryOp = MultivectorUnaryOp { (cls, _) =>
    GeneratedCode { code =>
      def makeMethod(result: MultiVector[Sym], firstLine: String, targetName: String | Null): Unit = {
        val resultCls = algebra.findMatchingClass(result)
        if (resultCls != algebra.zeroCls) {
          code("")
          if (targetName ne null) {
            code(s"@targetName(\"$targetName\")")
          }
          code(firstLine + s": ${resultCls.typeName} =")
          code.block {
            code(resultCls.makeConstructor(result))
          }
        }
      }

      val self = cls.self

      algebra.additionGroups.find(_.contains(cls)) match {
        case Some(group) =>
          for (pClass <- group) {
            val v = pClass.makeSymbolic("v")
            makeMethod(self + v, s"def +(v: ${pClass.typeName})", "plus")
            makeMethod(self - v, s"def -(v: ${pClass.typeName})", "minus")
            makeMethod(self + v * Sym("mult"), s"def madd(v: ${pClass.typeName}, mult: Double)", targetName = null)
          }
        case None =>
          val v = cls.makeSymbolic("v")
          makeMethod(self + v, s"def +(v: ${cls.typeName})", "plus")
          makeMethod(self - v, s"def -(v: ${cls.typeName})", "minus")
          makeMethod(self + v * Sym("mult"), s"def madd(v: ${cls.typeName}, mult: Double)", targetName = null)
      }

      // Unlike other operations, `scale` and `reciprocal` work on raw class fields, not on basis
      // blade coefficients: for classes whose fields differ from blade coefficients by sign
      // (e.g. vector/point trivectors) a blade-wise product would reintroduce those signs and
      // surprise users expecting plain componentwise arithmetic. They are intentionally NOT
      // consistent with any multivector operation. `reciprocal` pairs with `scale` to express
      // componentwise division: a.scale(b.reciprocal).
      if (!cls.isObject) {
        code("")
        code(s"def scale(v: ${cls.typeName}): ${cls.typeName} =")
        code.block {
          code(s"${cls.typeName}(")
          code.block {
            for (f <- cls.variableFields) {
              code(s"${f.name} = ${f.name} * v.${f.name},")
            }
          }
          code(")")
        }
        code("")
        code(s"def reciprocal: ${cls.typeName} =")
        code.block {
          code(s"${cls.typeName}(")
          code.block {
            for (f <- cls.variableFields) {
              code(s"${f.name} = 1.0 / ${f.name},")
            }
          }
          code(")")
        }
      }
    }
  }
