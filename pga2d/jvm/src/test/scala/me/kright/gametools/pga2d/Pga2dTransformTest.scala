package me.kright.gametools.pga2d

import me.kright.gametools.flatarray.FlatDoubleSerializer
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class Pga2dTransformTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private val eps = 1e-12

  test("sandwich and reverseSandwich for motor match motor") {
    forAll(Pga2dGenerators.anyMotors, Pga2dGenerators.anyMotors) { (motor, m2) =>
      val transform = Pga2dTransform(motor)
      assert((transform.sandwich(m2) - motor.sandwich(m2)).norm < eps)
      assert((transform.reverseSandwich(m2) - motor.reverseSandwich(m2)).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for rotor match motor") {
    forAll(Pga2dGenerators.anyMotors, Pga2dGenerators.rotors) { (motor, rotor) =>
      val transform = Pga2dTransform(motor)
      assert((transform.sandwich(rotor) - motor.sandwich(rotor)).norm < eps)
      assert((transform.reverseSandwich(rotor) - motor.reverseSandwich(rotor)).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for translator match motor") {
    forAll(Pga2dGenerators.anyMotors, Pga2dGenerators.translators) { (motor, t) =>
      val transform = Pga2dTransform(motor)
      assert((transform.sandwich(t) - motor.sandwich(t)).norm < eps)
      assert((transform.reverseSandwich(t) - motor.reverseSandwich(t)).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for projective translator match motor") {
    forAll(Pga2dGenerators.anyMotors, Pga2dGenerators.projectiveTranslators) { (motor, t) =>
      val transform = Pga2dTransform(motor)
      assert((transform.sandwich(t) - motor.sandwich(t)).norm < eps)
      assert((transform.reverseSandwich(t) - motor.reverseSandwich(t)).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for vector match motor") {
    forAll(Pga2dGenerators.anyMotors, Pga2dGenerators.vectors) { (motor, v) =>
      val transform = Pga2dTransform(motor)
      assert((transform.sandwich(v) - motor.sandwich(v)).norm < eps)
      assert((transform.reverseSandwich(v) - motor.reverseSandwich(v)).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for point match motor") {
    forAll(Pga2dGenerators.anyMotors, Pga2dGenerators.points) { (motor, p) =>
      val transform = Pga2dTransform(motor)
      assert((transform.sandwich(p) - motor.sandwich(p)).norm < eps)
      assert((transform.reverseSandwich(p) - motor.reverseSandwich(p)).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for projective point match motor") {
    forAll(Pga2dGenerators.anyMotors, Pga2dGenerators.projectivePoints) { (motor, p) =>
      val transform = Pga2dTransform(motor)
      assert((transform.sandwich(p) - motor.sandwich(p)).norm < eps)
      assert((transform.reverseSandwich(p) - motor.reverseSandwich(p)).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for line match motor") {
    forAll(Pga2dGenerators.anyMotors, Pga2dGenerators.lines) { (motor, line) =>
      val transform = Pga2dTransform(motor)
      assert((transform.sandwich(line) - motor.sandwich(line)).norm < eps)
      assert((transform.reverseSandwich(line) - motor.reverseSandwich(line)).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for central line match motor") {
    forAll(Pga2dGenerators.anyMotors, Pga2dGenerators.lineCentrals) { (motor, line) =>
      val transform = Pga2dTransform(motor)
      assert((transform.sandwich(line) - motor.sandwich(line)).norm < eps)
      assert((transform.reverseSandwich(line) - motor.reverseSandwich(line)).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for pseudoscalar match motor") {
    forAll(Pga2dGenerators.anyMotors, Pga2dGenerators.double1) { (motor, i) =>
      val transform = Pga2dTransform(motor)
      val p = Pga2dPseudoScalar(i)
      assert((transform.sandwich(p) - motor.sandwich(p)).norm < eps)
      assert((transform.reverseSandwich(p) - motor.reverseSandwich(p)).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for point center match motor") {
    forAll(Pga2dGenerators.anyMotors) { motor =>
      val transform = Pga2dTransform(motor)
      assert((transform.sandwich(Pga2dPointCenter) - motor.sandwich(Pga2dPointCenter)).norm < eps)
      assert((transform.reverseSandwich(Pga2dPointCenter) - motor.reverseSandwich(Pga2dPointCenter)).norm < eps)
    }
  }

  test("reverseSandwich is the inverse of sandwich for normalized motors") {
    forAll(Pga2dGenerators.normalizedMotors, Pga2dGenerators.projectivePoints) { (motor, p) =>
      val transform = Pga2dTransform(motor)
      val restored = transform.reverseSandwich(transform.sandwich(p))
      assert((restored - p).norm < 1e-9)
    }
  }

  test("flat serialization round-trip") {
    assert(FlatDoubleSerializer.getSize[Pga2dTransform] == Pga2dMotor.componentsCount + 7)

    forAll(Pga2dGenerators.anyMotors, Pga2dGenerators.anyMotors) { (m1, m2) =>
      val (a, b) = (Pga2dTransform(m1), Pga2dTransform(m2))
      val size = FlatDoubleSerializer.getSize[Pga2dTransform]
      val arr = new Array[Double](size * 2)
      FlatDoubleSerializer.write(a, arr, offset = 0)
      FlatDoubleSerializer.write(b, arr, offset = size)
      assert(FlatDoubleSerializer.read[Pga2dTransform](arr, offset = 0) == a)
      assert(FlatDoubleSerializer.read[Pga2dTransform](arr, offset = size) == b)
    }
  }

  test("equalsWithEps") {
    forAll(Pga2dGenerators.anyMotors) { motor =>
      val transform = Pga2dTransform(motor)
      assert(transform.equalsWithEps(transform, 0.0))
      assert(transform.equalsWithEps(Pga2dTransform(motor), 0.0))
      val shifted = Pga2dTransform(motor.copy(wx = motor.wx + 1.0))
      assert(!transform.equalsWithEps(shifted, 1e-3))
    }
  }

  test("id transform does not change objects") {
    forAll(Pga2dGenerators.projectivePoints) { p =>
      assert((Pga2dTransform.id.sandwich(p) - p).norm < eps)
      assert((Pga2dTransform.id.reverseSandwich(p) - p).norm < eps)
    }
  }
