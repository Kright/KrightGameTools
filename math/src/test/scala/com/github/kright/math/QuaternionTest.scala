package com.github.kright.math

import com.github.kright.math.MathGenerators.*
import com.github.kright.math.VectorMathGenerators.*
import org.scalacheck.Gen
import org.scalactic.{Equality, TolerantNumerics}
import org.scalatest.Assertions.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class QuaternionTest extends AnyFunSuite with ScalaCheckPropertyChecks:
  private implicit val eps: EqualityEps = EqualityEps(1e-12)

  test("quaternion pow") {
    forAll(normalizedQuaternions) { q =>
      assert((q * q) === (q ^ 2))
      assert((q * q * q) === (q ^ 3))

      val halfQ = q ^ 0.5
      assert(q === (halfQ * halfQ))

      assert((halfQ.getLog() * 2) === (q.getLog()))

      assert(q.conjugated() === (q ^ -1))
      assert(Quaternion() === (q ^ 0.0))
    }
  }

  test("quaternion slerp and lerp") {
    forAll(normalizedQuaternions, normalizedQuaternions) { (first, second) =>
      val lerp = Quaternion.lerp(first, second, 0.5).normalize()
      val slerp = Quaternion.slerp(first, second, 0.5)

      val groundTruth = first * (((first ^ -1) * second) ^ 0.5)

      assert(lerp === groundTruth)
      assert(slerp === groundTruth)
    }
  }

  test("quaternion log exp") {
    forAll(normalizedQuaternions) { q =>
      val exp = q.getLog()
      val q2 = Quaternion().setFromExp(exp)
      assert(q === q2)
    }
  }

  test("quaternion slerp") {
    forAll(normalizedQuaternions, normalizedQuaternions, Gen.double) { (first, second, t) =>
      val slerp = Quaternion.slerp(first, second, t)
      val groundTruth = first * (((first ^ -1) * second) ^ t)
      assert(slerp === groundTruth)
    }
  }

  test("get rotation axis and angle") {
    forAll(normalizedQuaternions) { q =>
      val axis = q.getRotationAxis()
      val angle = q.rotationAngleRadians()
      val reconstructed = Quaternion() := (angle, axis)
      assert(reconstructed === q)
    }
  }

  test("vector multiply by math definition") {
    forAll(normalizedQuaternions, vectors3InCube) { (q, v) =>
      val r1 = {
        val qqq = q * Quaternion(0.0, v.x, v.y, v.z) * q.conjugated()
        Vector3d(qqq.x, qqq.y, qqq.z)
      }

      val r2 = q * v
      assert(r1 === r2)
    }
  }

  test("magnitude multiplication") {
    implicit val doubleEquality: Equality[Double] = TolerantNumerics.tolerantDoubleEquality(0.000001)
    forAll(gaussianQuaternions, gaussianQuaternions) { (q0, q1) =>
      assert(q0.mag * q1.mag === (q0 * q1).mag)
    }
  }

  test("multiplication associativity") {
    forAll(normalizedQuaternions, normalizedQuaternions, normalizedQuaternions) { (first, second, third) =>
      val a = (first * second) * third
      val b = first * (second * third)
      assert(a === b)
    }
  }

  test("multiplication on vector associativity") {
    forAll(normalizedQuaternions, normalizedQuaternions, vectors3InCube) { (first, second, vec) =>
      val a = (first * second) * vec
      val b = first * (second * vec)
      assert(a === (b))
    }
  }

  test("quaternion to matrix and back") {
    forAll(normalizedQuaternions) { q =>
      val m3 = Matrix3d() := q
      val m4 = Matrix4d() := m3

      val r3 = Quaternion().setFromRotation(m3)
      val r4 = Quaternion().setFromRotation(m4)

      assert(q === r3, s"$q != $r3")
      assert(q === r4, s"$q != $r4")
    }
  }

  test("quaternion to euler and back") {
    forAll(normalizedQuaternions) { q =>
      val eq = Quaternion() := EulerAngles(q)
      assert(eq === q, s"$eq != $q")
    }
  }

  // todo fix bug
  test("quaternion rotation from axis to axis") {
    val eps = 1e-6
    implicit val doubleEquality: Equality[Double] = TolerantNumerics.tolerantDoubleEquality(eps)
    forAll(vectors3normalized, vectors3normalized, MinSuccessful(1)) { (sourceAxis, targetAxis) =>
      val q = Quaternion.fromAxisToAxis(sourceAxis, targetAxis)
      val q2 = Quaternion().setFromAxisToAxis(sourceAxis, targetAxis)

      assert(q === q2)
      assert((q * sourceAxis) === targetAxis)
      assert(q.mag === 1.0)
    }
  }

//  test("quaternion rotation from axis to axis has bug") {
//    val sourceAxis = Vector3d(1.0, 0.0, 0.0)
//    val targetAxis = Vector3d(-1.0, -0.0, -0.0)
//    val eps = 1e-6
//    implicit val doubleEquality: Equality[Double] = TolerantNumerics.tolerantDoubleEquality(eps)
//    val q = Quaternion.fromAxisToAxis(sourceAxis, targetAxis)
//    val q2 = Quaternion().setFromAxisToAxis(sourceAxis, targetAxis)
//
//    println(q)
//    println(q2)
//    assert(q === q2)
//    assert((q * sourceAxis) === targetAxis)
//    assert(q.mag === 1.0)
//  }

  test("quaternion rotation from axis over bisection") {
    val eps = 1e-6
    implicit val doubleEquality: Equality[Double] = TolerantNumerics.tolerantDoubleEquality(eps)
    forAll(vectors3normalized, vectors3normalized) { (sourceAxis, bisection) =>
      val halfQ = Quaternion.fromAxisToAxis(sourceAxis, bisection)

      val q = Quaternion.fromAxisOverBisection(sourceAxis, bisection)
      val q2 = Quaternion().setFromAxisOverBisection(sourceAxis, bisection)

      assert(q === q2)
      assert(q === (halfQ * halfQ), s"\nhalfQ = $halfQ,\nhalfQ^2=${halfQ * halfQ},\nq = $q,\ndiff = ${halfQ * halfQ - q}")
      assert(q.mag === 1.0)
    }
  }

  test("exp of zero is id quaternion") {
    val q = Quaternion().setFromExp(Vector3d(0.0, 0.0, 0.0))
    assert(q === Quaternion.id)
  }

  test("exp of very small value is id quaternion + value") {
    val eps = 1e-50

    val q = Quaternion().setFromExp(Vector3d(eps, 0.0, 0.0))
    assert(q.w == 1.0)
    assert(q.x == eps)
    assert(q.y == 0.0)
    assert(q.z == 0.0)
    assert(q.mag == 1.0)
  }