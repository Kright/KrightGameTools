package me.kright.gametools.pga.codegen.scalagen.pga3d.ops

import me.kright.gametools.ga.{MultiVector, PGA3}
import me.kright.gametools.pga.codegen.scalagen.common.{GeneratedCode, MultivectorUnaryOp}
import me.kright.gametools.pga.codegen.scalagen.pga3d.Pga3dScalaAlgebra.{bivector, bivectorBulk, bivectorWeight, motor, rotor, translator}
import me.kright.gametools.symbolic.Sym

object DefExpForBivector:
  private def sinDivLenCode(lenExpr: String): String =
    s"""val len = $lenExpr
       |val cos = Math.cos(len)
       |
       |// sin(x)/x = 1 - x^2/6 + x^4/120 - ...; at x <= 1e-5 the dropped x^4/120 <= 8.4e-23
       |// relative term is far below 1e-17, so the second-order form is exact in double
       |val sinDivLen = if (len > 1e-5) {
       |  Math.sin(len) / len
       |} else 1.0 - (len * len) / 6.0""".stripMargin

  private val sinMinusCosDivLen2Code: String =
    s"""// (sin(x)/x - cos(x)) / x^2, step by step:
       |//   sin(x)   = x - x^3/6 + x^5/120 - x^7/5040 + ...
       |//   sin(x)/x = 1 - x^2/6 + x^4/120 - x^6/5040 + ...
       |//   cos(x)   = 1 - x^2/2 + x^4/24  - x^6/720  + ...
       |//   sin(x)/x - cos(x) = (1/2 - 1/6)*x^2 + (1/120 - 1/24)*x^4 + (1/720 - 1/5040)*x^6 + ...
       |//                     = x^2/3 - x^4/30 + x^6/840 - ...
       |//   divide by x^2:      1/3   - x^2/30 + x^4/840 - ...
       |// at x <= 1e-5 the dropped x^4/840 <= 1.2e-23 is relatively far below 1e-17,
       |// so the second-order form is exact in double
       |val sinMinusCosDivLen2 = if (len > 1e-5) {
       |  (sinDivLen - cos) / (len * len)
       |} else 1.0 / 3.0 - (len * len) / 30.0""".stripMargin

  def apply()(using pga3: PGA3): MultivectorUnaryOp = MultivectorUnaryOp { (cls, v) =>
    GeneratedCode { code =>
      val self = cls.self
      if (cls == bivector) {
        {
          val IBdiv2 = self.bulk ^ self.weight
          val aIBettaDiv2 = self.geometric(IBdiv2)
          val result = MultiVector.scalar(Sym("cos")) + (self + IBdiv2) * Sym("sinDivLen") + aIBettaDiv2 * Sym("sinMinusCosDivLen2")

          code(s"\ndef exp: ${motor.name} =")
          code.block {
            code(
              sinDivLenCode("bulkNorm") + "\n\n" +
                sinMinusCosDivLen2Code + "\n\n" +
                motor.makeConstructor(result)
            )
          }
        }

        {
          val selfMulT = self * Sym("t")
          val IBdiv2 = selfMulT.bulk ^ selfMulT.weight
          val aIBettaDiv2 = selfMulT.geometric(IBdiv2)
          val result = MultiVector.scalar(Sym("cos")) + (selfMulT + IBdiv2) * Sym("sinDivLen") + aIBettaDiv2 * Sym("sinMinusCosDivLen2")

          code(s"\n/** for components below ~1e-154 the pairwise field products of this t-factored form may underflow; (b * t).exp does not */")
          code(s"def exp(t: Double): ${motor.name} =")
          code.block {
            code(
              sinDivLenCode("bulkNorm * Math.abs(t)") + "\n\n" +
                sinMinusCosDivLen2Code + "\n\n" +
                motor.makeConstructor(result)
            )
          }
        }
      }
      if (cls == bivectorBulk) {
        {
          val IBdiv2 = self.bulk ^ self.weight
          val aIBettaDiv2 = self.geometric(IBdiv2)
          val result = MultiVector.scalar(Sym("cos")) + (self + IBdiv2) * Sym("sinDivLen") + aIBettaDiv2 * Sym("sinMinusCosDivLen2")

          code(s"\ndef exp: ${rotor.typeName} =")
          code.block {
            code(
              sinDivLenCode("bulkNorm") + "\n\n" +
                rotor.makeConstructor(result)
            )
          }
        }

        {
          val selfMulT = self * Sym("t")
          val IBdiv2 = selfMulT.bulk ^ selfMulT.weight
          val aIBettaDiv2 = selfMulT.geometric(IBdiv2)
          val result = MultiVector.scalar(Sym("cos")) + (selfMulT + IBdiv2) * Sym("sinDivLen") + aIBettaDiv2 * Sym("sinMinusCosDivLen2")

          code(s"\ndef exp(t: Double): ${rotor.typeName} =")
          code.block {
            code(
              sinDivLenCode("bulkNorm * Math.abs(t)") + "\n\n" +
                rotor.makeConstructor(result)
            )
          }
        }
      }
      if (cls == bivectorWeight) {
        {
          val result = MultiVector.scalar(Sym(1.0)) + self
          code(s"\ndef exp: ${translator.typeName} =")
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
