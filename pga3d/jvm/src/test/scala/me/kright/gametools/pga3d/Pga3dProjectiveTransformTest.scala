package me.kright.gametools.pga3d

import me.kright.gametools.flatarray.FlatDoubleSerializer
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class Pga3dProjectiveTransformTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private val eps = 1e-12

  test("sandwich and reverseSandwich for motor match motor") {
    forAll(Pga3dGenerators.anyMotors, Pga3dGenerators.anyMotors) { (motor, m2) =>
      val transform = Pga3dProjectiveTransform(motor)
      assert((transform.sandwich(m2) - motor.sandwich(m2)).norm < eps)
      assert((transform.reverseSandwich(m2) - motor.reverseSandwich(m2)).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for rotor match motor") {
    forAll(Pga3dGenerators.anyMotors, Pga3dGenerators.rotors) { (motor, rotor) =>
      val transform = Pga3dProjectiveTransform(motor)
      assert((transform.sandwich(rotor) - motor.sandwich(rotor)).norm < eps)
      assert((transform.reverseSandwich(rotor) - motor.reverseSandwich(rotor)).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for translator match motor") {
    forAll(Pga3dGenerators.anyMotors, Pga3dGenerators.translators) { (motor, t) =>
      val transform = Pga3dProjectiveTransform(motor)
      assert((transform.sandwich(t) - motor.sandwich(t)).norm < eps)
      assert((transform.reverseSandwich(t) - motor.reverseSandwich(t)).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for projective translator match motor") {
    forAll(Pga3dGenerators.anyMotors, Pga3dGenerators.projectiveTranslators) { (motor, t) =>
      val transform = Pga3dProjectiveTransform(motor)
      assert((transform.sandwich(t) - motor.sandwich(t)).norm < eps)
      assert((transform.reverseSandwich(t) - motor.reverseSandwich(t)).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for bivector match motor") {
    forAll(Pga3dGenerators.anyMotors, Pga3dGenerators.bivectors) { (motor, b) =>
      val transform = Pga3dProjectiveTransform(motor)
      assert((transform.sandwich(b) - motor.sandwich(b)).norm < eps)
      assert((transform.reverseSandwich(b) - motor.reverseSandwich(b)).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for bivector bulk match motor") {
    forAll(Pga3dGenerators.anyMotors, Pga3dGenerators.bivectorBulks) { (motor, b) =>
      val transform = Pga3dProjectiveTransform(motor)
      assert((transform.sandwich(b) - motor.sandwich(b)).norm < eps)
      assert((transform.reverseSandwich(b) - motor.reverseSandwich(b)).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for bivector weight match motor") {
    forAll(Pga3dGenerators.anyMotors, Pga3dGenerators.bivectorWeight) { (motor, b) =>
      val transform = Pga3dProjectiveTransform(motor)
      assert((transform.sandwich(b) - motor.sandwich(b)).norm < eps)
      assert((transform.reverseSandwich(b) - motor.reverseSandwich(b)).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for vector match motor") {
    forAll(Pga3dGenerators.anyMotors, Pga3dGenerators.vectors) { (motor, v) =>
      val transform = Pga3dProjectiveTransform(motor)
      assert((transform.sandwich(v) - motor.sandwich(v)).norm < eps)
      assert((transform.reverseSandwich(v) - motor.reverseSandwich(v)).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for point match motor") {
    forAll(Pga3dGenerators.anyMotors, Pga3dGenerators.points) { (motor, p) =>
      val transform = Pga3dProjectiveTransform(motor)
      assert((transform.sandwich(p) - motor.sandwich(p)).norm < eps)
      assert((transform.reverseSandwich(p) - motor.reverseSandwich(p)).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for projective point match motor") {
    forAll(Pga3dGenerators.anyMotors, Pga3dGenerators.projectivePoints) { (motor, p) =>
      val transform = Pga3dProjectiveTransform(motor)
      assert((transform.sandwich(p) - motor.sandwich(p)).norm < eps)
      assert((transform.reverseSandwich(p) - motor.reverseSandwich(p)).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for pseudoscalar match motor") {
    forAll(Pga3dGenerators.anyMotors, Pga3dGenerators.double1) { (motor, i) =>
      val transform = Pga3dProjectiveTransform(motor)
      val p = Pga3dPseudoScalar(i)
      assert((transform.sandwich(p) - motor.sandwich(p)).norm < eps)
      assert((transform.reverseSandwich(p) - motor.reverseSandwich(p)).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for plane match motor") {
    forAll(Pga3dGenerators.anyMotors, Pga3dGenerators.planes) { (motor, p) =>
      val transform = Pga3dProjectiveTransform(motor)
      assert((transform.sandwich(p) - motor.sandwich(p)).norm < eps)
      assert((transform.reverseSandwich(p) - motor.reverseSandwich(p)).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for central plane match motor") {
    forAll(Pga3dGenerators.anyMotors, Pga3dGenerators.planeCentrals) { (motor, p) =>
      val transform = Pga3dProjectiveTransform(motor)
      assert((transform.sandwich(p) - motor.sandwich(p)).norm < eps)
      assert((transform.reverseSandwich(p) - motor.reverseSandwich(p)).norm < eps)
    }
  }

  test("sandwich and reverseSandwich for point center match motor") {
    forAll(Pga3dGenerators.anyMotors) { motor =>
      val transform = Pga3dProjectiveTransform(motor)
      assert((transform.sandwich(Pga3dPointCenter) - motor.sandwich(Pga3dPointCenter)).norm < eps)
      assert((transform.reverseSandwich(Pga3dPointCenter) - motor.reverseSandwich(Pga3dPointCenter)).norm < eps)
    }
  }

  test("reverseSandwich is the inverse of sandwich for normalized motors") {
    forAll(Pga3dGenerators.normalizedMotors, Pga3dGenerators.bivectors) { (motor, b) =>
      val transform = Pga3dProjectiveTransform(motor)
      val restored = transform.reverseSandwich(transform.sandwich(b))
      assert((restored - b).norm < 1e-9)
    }
  }

  test("flat serialization round-trip") {
    assert(FlatDoubleSerializer.getSize[Pga3dProjectiveTransform] == Pga3dMotor.componentsCount + 26)

    forAll(Pga3dGenerators.anyMotors, Pga3dGenerators.anyMotors) { (m1, m2) =>
      val (a, b) = (Pga3dProjectiveTransform(m1), Pga3dProjectiveTransform(m2))
      val size = FlatDoubleSerializer.getSize[Pga3dProjectiveTransform]
      val arr = new Array[Double](size * 2)
      FlatDoubleSerializer.write(a, arr, offset = 0)
      FlatDoubleSerializer.write(b, arr, offset = size)
      assert(FlatDoubleSerializer.read[Pga3dProjectiveTransform](arr, offset = 0) == a)
      assert(FlatDoubleSerializer.read[Pga3dProjectiveTransform](arr, offset = size) == b)
    }
  }

  test("equalsWithEps") {
    forAll(Pga3dGenerators.anyMotors) { motor =>
      val transform = Pga3dProjectiveTransform(motor)
      assert(transform.equalsWithEps(transform, 0.0))
      assert(transform.equalsWithEps(Pga3dProjectiveTransform(motor), 0.0))
      val shifted = Pga3dProjectiveTransform(motor.copy(wx = motor.wx + 1.0))
      assert(!transform.equalsWithEps(shifted, 1e-3))
    }
  }

  test("id transform does not change objects") {
    forAll(Pga3dGenerators.bivectors) { b =>
      assert((Pga3dProjectiveTransform.id.sandwich(b) - b).norm < eps)
      assert((Pga3dProjectiveTransform.id.reverseSandwich(b) - b).norm < eps)
    }
  }
