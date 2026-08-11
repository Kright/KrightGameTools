package me.kright.gametools.pga2d

import me.kright.gametools.flatarray.FlatDoubleSerializer
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class Pga2dTransformTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private val eps = 1e-12

  test("sandwich and reverseSandwich for motor match motor") {
    forAll(Pga2dGenerators.normalizedMotors, Pga2dGenerators.anyMotors) { (motor, m2) =>
      val transform = Pga2dTransform.fromNormalized(motor)
      assert((transform.sandwich(m2) - motor.sandwich(m2)).norm < eps)
      assert((transform.reverseSandwich(m2) - motor.reverseSandwich(m2)).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for rotor match motor") {
    forAll(Pga2dGenerators.normalizedMotors, Pga2dGenerators.rotors) { (motor, rotor) =>
      val transform = Pga2dTransform.fromNormalized(motor)
      assert((transform.sandwich(rotor) - motor.sandwich(rotor)).norm < eps)
      assert((transform.reverseSandwich(rotor) - motor.reverseSandwich(rotor)).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for translator match motor and stay translators") {
    forAll(Pga2dGenerators.normalizedMotors, Pga2dGenerators.translators) { (motor, t) =>
      val transform = Pga2dTransform.fromNormalized(motor)
      val moved: Pga2dTranslator = transform.sandwich(t)
      val movedBack: Pga2dTranslator = transform.reverseSandwich(t)
      assert((moved.toMotor - motor.sandwich(t).toMotor).norm < eps)
      assert((movedBack.toMotor - motor.reverseSandwich(t).toMotor).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for projective translator match motor") {
    forAll(Pga2dGenerators.normalizedMotors, Pga2dGenerators.projectiveTranslators) { (motor, t) =>
      val transform = Pga2dTransform.fromNormalized(motor)
      val moved: Pga2dProjectiveTranslator = transform.sandwich(t)
      val movedBack: Pga2dProjectiveTranslator = transform.reverseSandwich(t)
      assert((moved.toMotor - motor.sandwich(t).toMotor).norm < eps)
      assert((movedBack.toMotor - motor.reverseSandwich(t).toMotor).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for vector match motor") {
    forAll(Pga2dGenerators.normalizedMotors, Pga2dGenerators.vectors) { (motor, v) =>
      val transform = Pga2dTransform.fromNormalized(motor)
      assert((transform.sandwich(v) - motor.sandwich(v)).norm < eps)
      assert((transform.reverseSandwich(v) - motor.reverseSandwich(v)).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for point match motor and stay euclidean points") {
    forAll(Pga2dGenerators.normalizedMotors, Pga2dGenerators.points) { (motor, p) =>
      val transform = Pga2dTransform.fromNormalized(motor)
      val moved: Pga2dPoint = transform.sandwich(p)
      val movedBack: Pga2dPoint = transform.reverseSandwich(p)
      assert((moved.toProjectivePoint - motor.sandwich(p)).norm < eps)
      assert((movedBack.toProjectivePoint - motor.reverseSandwich(p)).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for projective point match motor") {
    forAll(Pga2dGenerators.normalizedMotors, Pga2dGenerators.projectivePoints) { (motor, p) =>
      val transform = Pga2dTransform.fromNormalized(motor)
      assert((transform.sandwich(p) - motor.sandwich(p)).norm < eps)
      assert((transform.reverseSandwich(p) - motor.reverseSandwich(p)).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for line match motor") {
    forAll(Pga2dGenerators.normalizedMotors, Pga2dGenerators.lines) { (motor, line) =>
      val transform = Pga2dTransform.fromNormalized(motor)
      assert((transform.sandwich(line) - motor.sandwich(line)).norm < eps)
      assert((transform.reverseSandwich(line) - motor.reverseSandwich(line)).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for central line match motor") {
    forAll(Pga2dGenerators.normalizedMotors, Pga2dGenerators.lineCentrals) { (motor, line) =>
      val transform = Pga2dTransform.fromNormalized(motor)
      assert((transform.sandwich(line) - motor.sandwich(line)).norm < eps)
      assert((transform.reverseSandwich(line) - motor.reverseSandwich(line)).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for pseudoscalar match motor") {
    forAll(Pga2dGenerators.normalizedMotors, Pga2dGenerators.double1) { (motor, i) =>
      val transform = Pga2dTransform.fromNormalized(motor)
      val p = Pga2dPseudoScalar(i)
      assert((transform.sandwich(p) - motor.sandwich(p)).norm < eps)
      assert((transform.reverseSandwich(p) - motor.reverseSandwich(p)).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for point center match motor and stay euclidean points") {
    forAll(Pga2dGenerators.normalizedMotors) { motor =>
      val transform = Pga2dTransform.fromNormalized(motor)
      val moved: Pga2dPoint = transform.sandwich(Pga2dPointCenter)
      val movedBack: Pga2dPoint = transform.reverseSandwich(Pga2dPointCenter)
      assert((moved.toProjectivePoint - motor.sandwich(Pga2dPointCenter)).norm < eps)
      assert((movedBack.toProjectivePoint - motor.reverseSandwich(Pga2dPointCenter)).norm < eps)
    }
  }

  test("apply renormalizes the motor: apply(m) == fromNormalized(m.renormalized)") {
    forAll(Pga2dGenerators.anyMotors) { motor =>
      assert(Pga2dTransform(motor) == Pga2dTransform.fromNormalized(motor.renormalized))
    }
  }

  test("reverseSandwich is the inverse of sandwich") {
    forAll(Pga2dGenerators.normalizedMotors, Pga2dGenerators.projectivePoints) { (motor, p) =>
      val transform = Pga2dTransform.fromNormalized(motor)
      val restored = transform.reverseSandwich(transform.sandwich(p))
      assert((restored - p).norm < 1e-9)
    }
  }

  test("flat serialization round-trip") {
    assert(FlatDoubleSerializer.getSize[Pga2dTransform] == Pga2dMotor.componentsCount + 6)

    forAll(Pga2dGenerators.normalizedMotors, Pga2dGenerators.normalizedMotors) { (m1, m2) =>
      val (a, b) = (Pga2dTransform.fromNormalized(m1), Pga2dTransform.fromNormalized(m2))
      val size = FlatDoubleSerializer.getSize[Pga2dTransform]
      val arr = new Array[Double](size * 2)
      FlatDoubleSerializer.write(a, arr, offset = 0)
      FlatDoubleSerializer.write(b, arr, offset = size)
      assert(FlatDoubleSerializer.read[Pga2dTransform](arr, offset = 0) == a)
      assert(FlatDoubleSerializer.read[Pga2dTransform](arr, offset = size) == b)
    }
  }

  test("equalsWithEps") {
    forAll(Pga2dGenerators.normalizedMotors) { motor =>
      val transform = Pga2dTransform.fromNormalized(motor)
      assert(transform.equalsWithEps(transform, 0.0))
      assert(transform.equalsWithEps(Pga2dTransform.fromNormalized(motor), 0.0))
      val shifted = Pga2dTransform.fromNormalized(motor.geometric(Pga2dTranslator.addVector(Pga2dVector(1.0, 0.0))))
      assert(!transform.equalsWithEps(shifted, 1e-3))
    }
  }

  test("id transform does not change objects") {
    forAll(Pga2dGenerators.projectivePoints) { p =>
      assert((Pga2dTransform.id.sandwich(p) - p).norm < eps)
      assert((Pga2dTransform.id.reverseSandwich(p) - p).norm < eps)
    }
  }
