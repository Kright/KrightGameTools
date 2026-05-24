package me.kright.gametools.ga

import me.kright.gametools.ga.PGA.translator
import me.kright.gametools.symbolic.Sym
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class PGATest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  /**
   * PGA3d: MultiVector(
   * 1 -> 1.0
   * wx -> -0.5 * d.x
   * wy -> -0.5 * d.y
   * wz -> -0.5 * d.z
   * )
   */
  test("translate") {
    PGAGenerator.forAnyPGA {
      val src = Sym.point("src")
      val dst = Sym.point("dst")

      val tr = translator(src, dst)

      assert(tr.sandwich(src).withoutZeros == dst)
      assert(tr.sandwich(dst).withoutZeros == dst + (dst - src))
    }
  }

  test("translate logarithm and power") {
    PGAGenerator.forAnyPGA {
      val src = Sym.point("src")
      val dst = Sym.point("dst")

      val tr = translator(src, dst)

      val power = Sym("power")
      val log = tr.weight
      val trPower = MultiVector.scalar[Sym](Sym.one) + tr.weight * power

      assert(trPower.sandwich(src).withoutZeros == src + (dst - src) * power)
    }
  }

  test("weight is bulk of weight") {
    PGAGenerator.forAnyPGA {
      val a = Sym.multiVector("a")
      require(a.weight == a.dual.bulk.dual)
    }
  }

