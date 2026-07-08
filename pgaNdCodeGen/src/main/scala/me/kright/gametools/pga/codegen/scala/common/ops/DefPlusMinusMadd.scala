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

      makeMethod(self.multiplyElementwise(cls.makeSymbolic("v")), s"def multiplyElementwise(v: ${cls.typeName})", targetName = null)
    }
  }
