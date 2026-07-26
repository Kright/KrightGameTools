package me.kright.gametools.pga3d

import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class ExponentTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  test("exp for bivector") {
    forAll(Pga3dGenerators.bivectors) { bivector =>
      assert((bivector.exp - bivector.exp(1.0)).norm < 1e-15)
      assert((bivector.bulk.exp - bivector.bulk.exp(1.0)).norm < 1e-15)
      assert((bivector.weight.exp - bivector.weight.exp(1.0)).norm < 1e-15)
    }
  }

  test("exp for full with t") {
    forAll(Pga3dGenerators.bivectors, Pga3dGenerators.double1) { (bivector, t) =>
      assert(((bivector * t).exp - bivector.exp(t)).norm < 1e-15)
      assert(((bivector.bulk * t).exp - bivector.bulk.exp(t)).norm < 1e-15)
      assert(((bivector.weight * t).exp - bivector.weight.exp(t)).norm < 1e-15)
    }
  }

  test("log is inverse of exp") {
    forAll(Pga3dGenerators.bivectors) { bivector =>
      assert((bivector - bivector.exp.log).norm < 1e-14)
      assert((bivector.bulk - bivector.bulk.exp.log).norm < 1e-14)
      assert((bivector.weight - bivector.weight.exp.log).norm < 1e-14)
    }
  }


  test("pow composes the motion") {
    forAll(Pga3dGenerators.normalizedMotors) { m =>
      assert((m.pow(2.0) - m.geometric(m)).norm < 1e-13, s"m = $m")
      val half = m.pow(0.5)
      val restored = half.geometric(half)
      assert(Math.min((restored - m).norm, (restored + m).norm) < 1e-13, s"m = $m")
    }
    forAll(Pga3dGenerators.normalizedRotors) { r =>
      val half = r.pow(0.5)
      val restored = half.geometric(half)
      assert(Math.min((restored - r).norm, (restored + r).norm) < 1e-13, s"r = $r")
    }
    forAll(Pga3dGenerators.translators) { tr =>
      assert((tr.pow(2.0) - tr.geometric(tr)).norm < 1e-13 * (1.0 + tr.norm), s"tr = $tr")
    }
  }

  test("log returns the principal branch for half-angle > pi/2") {
    // historical failing case: bulkNorm ~ 1.587 > pi/2, so motor.s = cos(bulkNorm) < 0.
    // log cannot return b itself here - it lands on the principal branch (flipping the
    // motor sign) - but the restored generator must produce the same motor up to sign
    val b = Pga3dBivector(-0.26426518006043076, -0.8856240901960408, -0.17870331716302612,
      0.9917926945307447, 0.936262951274583, 0.8114467770348108)
    assert(b.bulkNorm > Math.PI / 2)

    val motor = b.exp
    val restoredMotor = motor.log.exp
    val motorDiff = Math.min((restoredMotor - motor).norm, (restoredMotor + motor).norm)
    assert(motorDiff <= 1e-14 * motor.norm, s"motor = $motor, restored = $restoredMotor")
    assert(motor.log.bulkNorm <= Math.PI / 2)

    val rotor = b.bulk.exp
    val restoredRotor = rotor.log.exp
    val rotorDiff = Math.min((restoredRotor - rotor).norm, (restoredRotor + rotor).norm)
    assert(rotorDiff <= 1e-14 * rotor.norm, s"rotor = $rotor, restored = $restoredRotor")

    // the weight part is unaffected by the branch cut
    assert(b.weight.exp.log == b.weight)
  }

  test("bivector split") {
    forAll(Pga3dGenerators.bivectors.filter(_.bulkNorm > 1e-6)) { bivector =>
      val (line, shift) = bivector.split

      // the defining property: the parts sum back to the original bivector
      assert((line + shift - bivector).norm <= 1e-14 * bivector.norm, s"bivector = $bivector")
      assert(math.abs(line.weight.dual.dot(line.bulk)) < 1e-15)
    }
  }

  test("bivector split with negligible bulk returns the components unchanged") {
    // exercises the div < 1e-100 early return
    val b = Pga3dBivector(1.0, -2.0, 0.5, 1e-60, 0.0, -1e-60)
    val (line, shift) = b.split
    assert(line + shift == b)
    assert(shift == Pga3dBivectorWeight(1.0, -2.0, 0.5))
  }
