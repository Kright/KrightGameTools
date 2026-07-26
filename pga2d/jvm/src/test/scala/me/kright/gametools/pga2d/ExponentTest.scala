package me.kright.gametools.pga2d

import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class ExponentTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  test("exp() equals exp(1.0)") {
    forAll(Pga2dGenerators.projectivePoints) { p =>
      assert((p.exp - p.exp(1.0)).norm < 1e-15)
    }
    forAll(Pga2dGenerators.vectors) { v =>
      assert((v.exp - v.exp(1.0)).norm < 1e-15)
    }
  }

  test("exp with t") {
    forAll(Pga2dGenerators.projectivePoints, Pga2dGenerators.double1) { (p, t) =>
      assert(((p * t).exp - p.exp(t)).norm < 1e-15)
    }
    forAll(Pga2dGenerators.vectors, Pga2dGenerators.double1) { (v, t) =>
      assert(((v * t).exp - v.exp(t)).norm < 1e-15)
    }
  }

  test("log is inverse of exp") {
    // generator components are in [-1, 1], so the rotation angle |xy| <= 1 < pi
    forAll(Pga2dGenerators.projectivePoints) { p =>
      assert((p - p.exp.log).norm < 1e-15)
    }
  }

  test("exp of grade-2 element is a normalized motor") {
    forAll(Pga2dGenerators.projectivePoints) { p =>
      val motor = p.exp
      val mm = motor.geometric(motor.reverse)
      assert((mm - Pga2dMotor(s = 1.0)).norm < 1e-14)
    }
  }

  test("pow composes the motion") {
    forAll(Pga2dGenerators.normalizedMotors) { m =>
      assert((m.pow(2.0) - m.geometric(m)).norm < 1e-13, s"m = $m")
      val half = m.pow(0.5)
      val restored = half.geometric(half)
      assert(Math.min((restored - m).norm, (restored + m).norm) < 1e-13, s"m = $m")
    }
    forAll(Pga2dGenerators.normalizedRotors) { r =>
      val half = r.pow(0.5)
      val restored = half.geometric(half)
      assert(Math.min((restored - r).norm, (restored + r).norm) < 1e-13, s"r = $r")
    }
    forAll(Pga2dGenerators.translators) { tr =>
      assert((tr.pow(2.0) - tr.geometric(tr)).norm < 1e-13 * (1.0 + tr.norm), s"tr = $tr")
    }
  }
