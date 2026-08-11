package me.kright.gametools.pga3d

import me.kright.gametools.flatarray.FlatDoubleSerializer
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class Pga3dTransformTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private val eps = 1e-12

  test("sandwich and reverseSandwich for motor match motor") {
    forAll(Pga3dGenerators.normalizedMotors, Pga3dGenerators.anyMotors) { (motor, m2) =>
      val transform = Pga3dTransform.fromNormalized(motor)
      assert((transform.sandwich(m2) - motor.sandwich(m2)).norm < eps)
      assert((transform.reverseSandwich(m2) - motor.reverseSandwich(m2)).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for rotor match motor") {
    forAll(Pga3dGenerators.normalizedMotors, Pga3dGenerators.rotors) { (motor, rotor) =>
      val transform = Pga3dTransform.fromNormalized(motor)
      assert((transform.sandwich(rotor) - motor.sandwich(rotor)).norm < eps)
      assert((transform.reverseSandwich(rotor) - motor.reverseSandwich(rotor)).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for translator match motor and stay translators") {
    forAll(Pga3dGenerators.normalizedMotors, Pga3dGenerators.translators) { (motor, t) =>
      val transform = Pga3dTransform.fromNormalized(motor)
      val moved: Pga3dTranslator = transform.sandwich(t)
      val movedBack: Pga3dTranslator = transform.reverseSandwich(t)
      assert((moved.toMotor - motor.sandwich(t)).norm < eps)
      assert((movedBack.toMotor - motor.reverseSandwich(t)).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for projective translator match motor") {
    forAll(Pga3dGenerators.normalizedMotors, Pga3dGenerators.projectiveTranslators) { (motor, t) =>
      val transform = Pga3dTransform.fromNormalized(motor)
      val moved: Pga3dProjectiveTranslator = transform.sandwich(t)
      val movedBack: Pga3dProjectiveTranslator = transform.reverseSandwich(t)
      assert((moved.toMotor - motor.sandwich(t)).norm < eps)
      assert((movedBack.toMotor - motor.reverseSandwich(t)).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for bivector match motor") {
    forAll(Pga3dGenerators.normalizedMotors, Pga3dGenerators.bivectors) { (motor, b) =>
      val transform = Pga3dTransform.fromNormalized(motor)
      assert((transform.sandwich(b) - motor.sandwich(b)).norm < eps)
      assert((transform.reverseSandwich(b) - motor.reverseSandwich(b)).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for bivector bulk match motor") {
    forAll(Pga3dGenerators.normalizedMotors, Pga3dGenerators.bivectorBulks) { (motor, b) =>
      val transform = Pga3dTransform.fromNormalized(motor)
      assert((transform.sandwich(b) - motor.sandwich(b)).norm < eps)
      assert((transform.reverseSandwich(b) - motor.reverseSandwich(b)).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for bivector weight match motor") {
    forAll(Pga3dGenerators.normalizedMotors, Pga3dGenerators.bivectorWeight) { (motor, b) =>
      val transform = Pga3dTransform.fromNormalized(motor)
      assert((transform.sandwich(b) - motor.sandwich(b)).norm < eps)
      assert((transform.reverseSandwich(b) - motor.reverseSandwich(b)).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for vector match motor") {
    forAll(Pga3dGenerators.normalizedMotors, Pga3dGenerators.vectors) { (motor, v) =>
      val transform = Pga3dTransform.fromNormalized(motor)
      assert((transform.sandwich(v) - motor.sandwich(v)).norm < eps)
      assert((transform.reverseSandwich(v) - motor.reverseSandwich(v)).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for point match motor and stay euclidean points") {
    forAll(Pga3dGenerators.normalizedMotors, Pga3dGenerators.points) { (motor, p) =>
      val transform = Pga3dTransform.fromNormalized(motor)
      val moved: Pga3dPoint = transform.sandwich(p)
      val movedBack: Pga3dPoint = transform.reverseSandwich(p)
      assert((moved.toProjectivePoint - motor.sandwich(p)).norm < eps)
      assert((movedBack.toProjectivePoint - motor.reverseSandwich(p)).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for projective point match motor") {
    forAll(Pga3dGenerators.normalizedMotors, Pga3dGenerators.projectivePoints) { (motor, p) =>
      val transform = Pga3dTransform.fromNormalized(motor)
      assert((transform.sandwich(p) - motor.sandwich(p)).norm < eps)
      assert((transform.reverseSandwich(p) - motor.reverseSandwich(p)).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for pseudoscalar match motor") {
    forAll(Pga3dGenerators.normalizedMotors, Pga3dGenerators.double1) { (motor, i) =>
      val transform = Pga3dTransform.fromNormalized(motor)
      val p = Pga3dPseudoScalar(i)
      assert((transform.sandwich(p) - motor.sandwich(p)).norm < eps)
      assert((transform.reverseSandwich(p) - motor.reverseSandwich(p)).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for plane match motor") {
    forAll(Pga3dGenerators.normalizedMotors, Pga3dGenerators.planes) { (motor, p) =>
      val transform = Pga3dTransform.fromNormalized(motor)
      assert((transform.sandwich(p) - motor.sandwich(p)).norm < eps)
      assert((transform.reverseSandwich(p) - motor.reverseSandwich(p)).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for central plane match motor") {
    forAll(Pga3dGenerators.normalizedMotors, Pga3dGenerators.planeCentrals) { (motor, p) =>
      val transform = Pga3dTransform.fromNormalized(motor)
      assert((transform.sandwich(p) - motor.sandwich(p)).norm < eps)
      assert((transform.reverseSandwich(p) - motor.reverseSandwich(p)).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for point center match motor and stay euclidean points") {
    forAll(Pga3dGenerators.normalizedMotors) { motor =>
      val transform = Pga3dTransform.fromNormalized(motor)
      val moved: Pga3dPoint = transform.sandwich(Pga3dPointCenter)
      val movedBack: Pga3dPoint = transform.reverseSandwich(Pga3dPointCenter)
      assert((moved.toProjectivePoint - motor.sandwich(Pga3dPointCenter)).norm < eps)
      assert((movedBack.toProjectivePoint - motor.reverseSandwich(Pga3dPointCenter)).norm < eps)
    }
  }

  test("apply renormalizes the motor: apply(m) == fromNormalized(m.renormalized)") {
    forAll(Pga3dGenerators.anyMotors) { motor =>
      assert(Pga3dTransform(motor) == Pga3dTransform.fromNormalized(motor.renormalized))
    }
  }

  test("apply of a scaled motor equals the transform of the normalized one") {
    forAll(Pga3dGenerators.normalizedMotors, Pga3dGenerators.points) { (motor, p) =>
      val scaled = Pga3dTransform(motor * 3.5)
      val expected = Pga3dTransform.fromNormalized(motor)
      assert((scaled.sandwich(p).toProjectivePoint - expected.sandwich(p).toProjectivePoint).norm < 1e-9)
    }
  }

  test("reverseSandwich is the inverse of sandwich") {
    forAll(Pga3dGenerators.normalizedMotors, Pga3dGenerators.bivectors) { (motor, b) =>
      val transform = Pga3dTransform.fromNormalized(motor)
      val restored = transform.reverseSandwich(transform.sandwich(b))
      assert((restored - b).norm < 1e-9)
    }
  }

  test("flat serialization round-trip") {
    assert(FlatDoubleSerializer.getSize[Pga3dTransform] == Pga3dMotor.componentsCount + 24)

    forAll(Pga3dGenerators.normalizedMotors, Pga3dGenerators.normalizedMotors) { (m1, m2) =>
      val (a, b) = (Pga3dTransform.fromNormalized(m1), Pga3dTransform.fromNormalized(m2))
      val size = FlatDoubleSerializer.getSize[Pga3dTransform]
      val arr = new Array[Double](size * 2)
      FlatDoubleSerializer.write(a, arr, offset = 0)
      FlatDoubleSerializer.write(b, arr, offset = size)
      assert(FlatDoubleSerializer.read[Pga3dTransform](arr, offset = 0) == a)
      assert(FlatDoubleSerializer.read[Pga3dTransform](arr, offset = size) == b)
    }
  }

  test("equalsWithEps") {
    forAll(Pga3dGenerators.normalizedMotors) { motor =>
      val transform = Pga3dTransform.fromNormalized(motor)
      assert(transform.equalsWithEps(transform, 0.0))
      assert(transform.equalsWithEps(Pga3dTransform.fromNormalized(motor), 0.0))
      val shifted = Pga3dTransform.fromNormalized(motor.geometric(Pga3dTranslator.addVector(Pga3dVector(1.0, 0.0, 0.0))))
      assert(!transform.equalsWithEps(shifted, 1e-3))
    }
  }

  test("id transform does not change objects") {
    forAll(Pga3dGenerators.bivectors) { b =>
      assert((Pga3dTransform.id.sandwich(b) - b).norm < eps)
      assert((Pga3dTransform.id.reverseSandwich(b) - b).norm < eps)
    }
  }
