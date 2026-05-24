package me.kright.gametools.ga

import me.kright.gametools.vector.VectorMathGenerators
import me.kright.gametools.symbolic.Sym
import org.scalactic.{Equality, TolerantNumerics}
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class GATest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  test("dual is inverse of dual") {
    for (ga <- Seq(
      PGA2(GARepresentationConfig.e0123(Signature.pga2)),
      PGA3(GARepresentationConfig.e0123(Signature.pga3)),
      GA.pga2,
      GA.pga3,
      GA.ga2,
      GA.ga3,
      GA.ga4
    )) {
      ga.use {
        val v = Sym.multiVector("v")
        assert(v === v.dual.dual)
      }
    }
  }

  test("rightComplement is inversion of leftComplement") {
    GAGenerator.forAnyGA {
      val a = Sym.multiVector("a")
      assert(a === a.leftComplement.rightComplement)
      assert(a === a.rightComplement.leftComplement)
    }
  }

  test("rightComplement power 4 is id") {
    GAGenerator.forAnyGA {
      val a = Sym.multiVector("a")
      assert(a === a.rightComplement.rightComplement.rightComplement.rightComplement)
    }
  }

  test("exponent seriers") {
    given doubleEquality: Equality[Double] = TolerantNumerics.tolerantDoubleEquality(1e-9)

    GAGenerator.forAnyGA {
      val one = MultiVector.scalar(1.0)

      assert(one.exponentSeries.take(20).reduce(_ + _).norm === Math.E)
      assert(one.exponentBySeriesSum(1e-11).norm === Math.E)
    }
  }

  test("exp for zero bivector") {
    GAGenerator.forAnyGA {
      if (ga.signature.positives <= 3 && ga.signature.zeros <= 1) {
        val zero = MultiVector[Double](ga.blades.map(b => b -> 0.0))
        val one = MultiVector.scalar[Double](1.0)

        assert(zero.exponentBySeriesSum(1e-15).withoutZeros == one)
        assert(PGA.expForBivector(zero).withoutZeros == one)
      }
    }
  }

  test("exp for bivector for near-zero vectors") {
    forAll(
      GAGenerator.allGa
        .filter(ga => ga.signature.positives <= 3 && ga.signature.zeros <= 1)
        .flatMap(ga => GAGenerator.bladesGen(grade = 2)(using ga)),
      VectorMathGenerators.doubleInRange(-100.0, 0.0),
      MinSuccessful(100)) { (bv, power) =>
      bv.ga.use {
        val v = bv * Math.pow(10, power)
        val vExp = PGA.expForBivector(v)

        assert((v.exponentBySeriesSum(1e-17, maxSteps = 1000) - vExp).norm < 1e-15,
          s"""
             |exponentBySeriesSum != expForBivector
             |ga = ${ga.signature}
             |v.norm = ${v.norm}
             |v = $v""".stripMargin)

        assert(((vExp geometric vExp) - PGA.expForBivector(v * 2.0)).norm < 1e-15,
          s"""
             |exp(v)**2 != exp(v * 2.0)
             |ga = ${ga.signature}
             |v.norm = ${v.norm}
             |v = $v""".stripMargin)
      }
    }
  }

  test("dual of geometric is antigeometric of dual") {
    forAll(GAGenerator.allGa) { ga =>
      ga.use {
        val a = Sym.multiVector("a")
        val b = Sym.multiVector("b")
        assert(a.geometric(b).dual == a.dual.antiGeometric(b.dual))
      }
    }
  }
