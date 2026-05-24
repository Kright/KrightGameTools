package com.github.kright.pga3d.codegen.scala.ops

import me.kright.gametools.ga.{MultiVector, PGA3}
import com.github.kright.pga3d.codegen.scala.{GeneratedCode, ScalaMultivectorSubClass, MultivectorUnaryOp}
import com.github.kright.pga3d.codegen.scala.ScalaMultivectorSubClass.{motor, quaternion, translator}
import com.github.kright.symbolic.Sym

object DefExpForBivector:
  def apply()(using pga3: PGA3): MultivectorUnaryOp = MultivectorUnaryOp { (cls, v) =>
    GeneratedCode { code =>
      val self = cls.self
      if (cls == ScalaMultivectorSubClass.bivector) {
        {
          val IBdiv2 = self.bulk ^ self.weight
          val aIBettaDiv2 = self.geometric(IBdiv2)
          val result = MultiVector.scalar(Sym("cos")) + (self + IBdiv2) * Sym("sinDivLen") + aIBettaDiv2 * Sym("sinMinusCosDivLen2")

          code(s"\ndef exp(): ${ScalaMultivectorSubClass.motor.name} =")
          code.block {
            code(
              s"""val len = bulkNorm
                 |val cos = Math.cos(len)
                 |
                 |val sinDivLen = if (len > 1e-5) {
                 |  Math.sin(len) / len
                 |} else 1.0 - (len * len) / 6.0
                 |
                 |val sinMinusCosDivLen2 = if (len > 1e-5) {
                 |  (sinDivLen - cos) / (len * len)
                 |} else (1.0 / 3.0) * (1.0 + 0.8 * len * len)
                 |
                 |${motor.makeConstructor(result)}""".stripMargin
            )
          }
        }

        {
          val selfMulT = self * Sym("t")
          val IBdiv2 = selfMulT.bulk ^ selfMulT.weight
          val aIBettaDiv2 = selfMulT.geometric(IBdiv2)
          val result = MultiVector.scalar(Sym("cos")) + (selfMulT + IBdiv2) * Sym("sinDivLen") + aIBettaDiv2 * Sym("sinMinusCosDivLen2")

          code(s"\ndef exp(t: Double): ${ScalaMultivectorSubClass.motor.name} =")
          code.block {
            code(
              s"""val len = bulkNorm * Math.abs(t)
                 |val cos = Math.cos(len)
                 |
                 |val sinDivLen = if (len > 1e-5) {
                 |  Math.sin(len) / len
                 |} else 1.0 - (len * len) / 6.0
                 |
                 |val sinMinusCosDivLen2 = if (len > 1e-5) {
                 |  (sinDivLen - cos) / (len * len)
                 |} else (1.0 / 3.0) * (1.0 + 0.8 * len * len)
                 |
                 |${motor.makeConstructor(result)}""".stripMargin
            )
          }
        }
      }
      if (cls == ScalaMultivectorSubClass.bivectorBulk) {
        {
          val IBdiv2 = self.bulk ^ self.weight
          val aIBettaDiv2 = self.geometric(IBdiv2)
          val result = MultiVector.scalar(Sym("cos")) + (self + IBdiv2) * Sym("sinDivLen") + aIBettaDiv2 * Sym("sinMinusCosDivLen2")

          code(s"\ndef exp(): ${quaternion.typeName} =")
          code.block {
            code(
              s"""val len = bulkNorm
                 |val cos = Math.cos(len)
                 |
                 |val sinDivLen = if (len > 1e-5) {
                 |  Math.sin(len) / len
                 |} else 1.0 - (len * len) / 6.0
                 |
                 |${quaternion.makeConstructor(result)}""".stripMargin)
          }
        }

        {
          val selfMulT = self * Sym("t")
          val IBdiv2 = selfMulT.bulk ^ selfMulT.weight
          val aIBettaDiv2 = selfMulT.geometric(IBdiv2)
          val result = MultiVector.scalar(Sym("cos")) + (selfMulT + IBdiv2) * Sym("sinDivLen") + aIBettaDiv2 * Sym("sinMinusCosDivLen2")

          code(s"\ndef exp(t: Double): ${quaternion.typeName} =")
          code.block {
            code(
              s"""val len = bulkNorm * Math.abs(t)
                 |val cos = Math.cos(len)
                 |
                 |val sinDivLen = if (len > 1e-5) {
                 |  Math.sin(len) / len
                 |} else 1.0 - (len * len) / 6.0
                 |
                 |${quaternion.makeConstructor(result)}""".stripMargin)
          }
        }
      }
      if (cls == ScalaMultivectorSubClass.bivectorWeight) {
        {
          val result = MultiVector.scalar(Sym(1.0)) + self
          code(s"\ndef exp(): ${translator.typeName} =")
          code.block {
            code(translator.makeConstructor(result))
          }
        }

        {
          val result = MultiVector.scalar(Sym(1.0)) + self * Sym("t")
          code(s"\ndef exp(t: Double): ${translator.typeName} =")
          code.block {
            code(translator.makeConstructor(result))
          }
        }
      }
    }
  }
