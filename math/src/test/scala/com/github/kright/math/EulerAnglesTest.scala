package com.github.kright.math

import com.github.kright.math.MathGenerators.*
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
      val m = Matrix3d() := euler

      val mYaw = Matrix3d() := (Quaternion(euler.yaw, Vector3d(0, 1, 0)))
      val mPitch = Matrix3d() := (Quaternion(euler.pitch, Vector3d(1, 0, 0)))
      val mRoll = Matrix3d() := (Quaternion(euler.roll, Vector3d(0, 0, 1)))

      val mmm = mYaw * mPitch * mRoll
      assert(m === mmm)
    }
  }

  test("euler matrix quaternion correspondence") {
    forAll(eulerAngles) { euler =>
      val ma = Matrix3d() := euler
      val mb = Matrix3d() := (Quaternion(euler))
      assert(ma === mb)
      assert((Matrix4d() := euler) === (Matrix4d() := mb))
    }
  }

  test("zero angles") {
    val euler = EulerAngles(0, 0, 0)
    assert((Quaternion(euler)) === Quaternion.id)
    assert((Matrix3d() := euler) === Matrix3d().setIdentity())
    assert((Matrix4d() := euler) === Matrix4d().setIdentity())
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
    val e3 = EulerAngles(Matrix3d() := eulerAngles)
    val e4 = EulerAngles(Matrix4d() := eulerAngles)
    val eq = EulerAngles(Quaternion(eulerAngles))

    assert(e3 === eulerAngles, s"$e3 != $eulerAngles")
    assert(e4 === eulerAngles, s"$e4 != $eulerAngles")
    assert(eq === eulerAngles, s"$eq != $eulerAngles")
  }
