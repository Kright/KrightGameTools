package me.kright.gametools.pga3d.codegen.scala.ops

import me.kright.gametools.ga.{MultiVector, PGA3}
import me.kright.gametools.pga3d.codegen.scala.{GeneratedCode, ScalaMultivectorSubClass, MultivectorUnaryOp}
import me.kright.gametools.pga3d.codegen.scala.ScalaMultivectorSubClass.*
import me.kright.gametools.symbolic.Sym

object DefPlusMinusMadd:
  def apply()(using pga3: PGA3): MultivectorUnaryOp = MultivectorUnaryOp { (cls, s) =>
    GeneratedCode { code =>
      def makeMethod(result: MultiVector[Sym], firstLine: String, targetName: String | Null): Unit = {
        val resultCls = ScalaMultivectorSubClass.findMatchingClass(result)
        if (resultCls != zeroCls) {
          code("")
          if (targetName != null) {
            code(s"@targetName(\"$targetName\")")
          }
          code(firstLine + s": ${resultCls.typeName} =")
          code.block {
            code(resultCls.makeConstructor(result))
          }
        }
      }

      val self = cls.self
      val points = Set(projectivePoint, point, vector)
      val bivectors = Set(bivector, bivectorBulk, bivectorWeight)
      if (points.contains(cls)) {
        for (pClass <- points) {
          val v = pClass.makeSymbolic("v")
          makeMethod(self + v, s"def +(v: ${pClass.typeName})", "plus")
          makeMethod(self - v, s"def -(v: ${pClass.typeName})", "minus")
          makeMethod(self + v * Sym("mult"), s"def madd(v: ${pClass.typeName}, mult: Double)", targetName = null)
        }
      } else if (bivectors.contains(cls)) {
        for (pClass <- bivectors) {
          val v = pClass.makeSymbolic("v")
          makeMethod(self + v, s"def +(v: ${pClass.typeName})", "plus")
          makeMethod(self - v, s"def -(v: ${pClass.typeName})", "minus")
          makeMethod(self + v * Sym("mult"), s"def madd(v: ${pClass.typeName}, mult: Double)", targetName = null)
        }
      } else {
        val v = cls.makeSymbolic("v")
        makeMethod(self + v, s"def +(v: ${cls.typeName})", "plus")
        makeMethod(self - v, s"def -(v: ${cls.typeName})", "minus")
        makeMethod(self + v * Sym("mult"), s"def madd(v: ${cls.typeName}, mult: Double)", targetName = null)
      }

      makeMethod(self.multiplyElementwise(cls.makeSymbolic("v")), s"def multiplyElementwise(v: ${cls.typeName})", targetName = null)
    }
  }
