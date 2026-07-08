package me.kright.gametools.pga2d

import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class Pga2dMotorTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  test("motor renormalization") {
    val one = Pga2dMotor(s = 1.0)

    forAll(Pga2dGenerators.anyMotors.filter(_.bulkNorm > 0.01).map(_.renormalized), MinSuccessful(1000)) { renormalized =>
      val mm = renormalized.geometric(renormalized.reverse)
      assert((mm - one).norm < 5e-15, s"mm = ${mm}")
    }
  }

  test("renormalization is a uniform scale") {
    // renormalized rescales so that motor.geometric(motor.reverse) == 1, i.e. divides by bulkNorm
    forAll(Pga2dGenerators.anyMotors.filter(_.bulkNorm > 0.01), MinSuccessful(1000)) { motor =>
      val expected = motor / motor.bulkNorm
      assert((motor.renormalized - expected).norm < 1e-13)
    }
  }

  test("motor to rotor and translator") {
    forAll(Pga2dGenerators.normalizedMotors, MinSuccessful(1000)) { motor =>
      val (r, t) = motor.toRotorAndTranslator
      val restored = r.geometric(t)
      assert((restored - motor).norm < 1e-15)
    }
  }

  test("motor to translator and rotor") {
    forAll(Pga2dGenerators.normalizedMotors, MinSuccessful(1000)) { motor =>
      val (t, r) = motor.toTranslatorAndRotor
      val restored = t.geometric(r)
      assert((restored - motor).norm < 1e-15)
    }
  }

  test("normalized motor preserves distance between points") {
    forAll(Pga2dGenerators.normalizedMotors, Pga2dGenerators.points, Pga2dGenerators.points, MinSuccessful(1000)) { (motor, p1, p2) =>
      val d1 = p1.distanceTo(p2)
      val d2 = motor.sandwich(p1).toPoint.distanceTo(motor.sandwich(p2).toPoint)
      assert(Math.abs(d1 - d2) < 1e-9)
    }
  }

  test("motor log exp round trip") {
    forAll(Pga2dGenerators.normalizedMotors, MinSuccessful(1000)) { motor =>
      val restored = motor.log().exp()
      val diff1 = (restored - motor).norm
      val diff2 = (restored + motor).norm
      assert(Math.min(diff1, diff2) < 1e-14, s"motor = $motor, restored = $restored")
    }
  }

  test("motor id does nothing") {
    forAll(Pga2dGenerators.points) { p =>
      assert((Pga2dMotor.id.sandwich(p).toMultivector - p.toMultivector).norm < 1e-15)
    }
  }

  test("addVector on motor object") {
    forAll(Pga2dGenerators.vectors, Pga2dGenerators.points) { (v, p) =>
      val motor = Pga2dMotor.addVector(v)
      assert((motor.sandwich(p).toMultivector - (p + v).toMultivector).norm < 1e-14)
    }
  }
