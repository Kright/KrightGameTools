package com.github.kright.math

import com.github.kright.math.MathGenerators.*
import com.github.kright.matrix.{Matrix3d, Matrix4d}
import org.scalatest.Assertions.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class EulerAnglesTest extends AnyFunSuite with ScalaCheckPropertyChecks:
  private implicit val eps: EqualityEps = EqualityEps(1e-12)

  test("euler to quaternion conversion") {
    forAll(eulerAngles) { euler =>
      val q = Quaternion(euler)

      val qYaw = Quaternion(euler.yaw, Vector3d(0, 1, 0))
      val qPitch = Quaternion(euler.pitch, Vector3d(1, 0, 0))
      val qRoll = Quaternion(euler.roll, Vector3d(0, 0, 1))

      val qqq = qYaw * qPitch * qRoll
      assert(q === qqq)
    }
  }

  test("matrix to quaternion conversion") {
    forAll(eulerAngles) { euler =>
      val m = euler.toMatrix

      val mYaw = Quaternion(euler.yaw, Vector3d(0, 1, 0)).toMatrix
      val mPitch = Quaternion(euler.pitch, Vector3d(1, 0, 0)).toMatrix
      val mRoll = Quaternion(euler.roll, Vector3d(0, 0, 1)).toMatrix

      val mmm = mYaw * mPitch * mRoll
      assert(m === mmm)
    }
  }

  test("euler matrix quaternion correspondence") {
    forAll(eulerAngles) { euler =>
      val ma = euler.toMatrix
      val mb = Quaternion(euler).toMatrix
      assert(ma === mb)
    }
  }

  test("zero angles") {
    val euler = EulerAngles(0, 0, 0)
    assert((Quaternion(euler)) === Quaternion.id)
    assert(euler.toMatrix === Matrix3d.id)
  }

  test("up in euler to matrix/quaternion and back") {
    check(EulerAngles(0.55, Math.PI * 0.5, 0.0))
  }

  test("down in euler to matrix/quaternion and back") {
    check(EulerAngles(0.55, -Math.PI * 0.5, 0.0))
  }

  test("euler to matrix/quaternion and back") {
    forAll(eulerAngles) { euler =>
      if ((euler.pitch <= 0.999) && (euler.pitch >= -0.999)) {
        check(euler)
      }
    }
  }

  private def check(eulerAngles: EulerAngles): Unit = {
    val e3 = EulerAngles(eulerAngles.toMatrix)
    val eq = EulerAngles(Quaternion(eulerAngles))

    assert(e3 === eulerAngles, s"$e3 != $eulerAngles")
    assert(eq === eulerAngles, s"$eq != $eulerAngles")
  }
