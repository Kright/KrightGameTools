package com.github.kright.math

import com.github.kright.math.MathGenerators.*
import com.github.kright.math.VectorMathGenerators.*
import org.scalactic.{Equality, TolerantNumerics}
import org.scalatest.Assertions.*
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class MatrixTest extends AnyFunSuite with ScalaCheckPropertyChecks:
  private implicit val eps: EqualityEps = EqualityEps(1e-12)

  test("matrix and quaternion multiplication consistency") {
    forAll(normalizedQuaternions, vectors3InCube) { case (q, v) =>
      val mat3 = Matrix3d() := q
      val mat4 = Matrix4d() := q
      assert((mat3 * v) === (q * v))
      assert((mat4 * v) === (q * v))

      mat3.setRows(q.oX, q.oY, q.oZ)
      assert((mat3 * v) === (q * v))
    }
  }

  test("matrix and quaternion same multiplication order") {
    forAll(normalizedQuaternions, normalizedQuaternions) { case (q1, q2) =>
      val ma = Matrix3d() := (q1 * q2)
      val mb = (Matrix3d() := q1) * (Matrix3d() := q2)
      assert(ma === (mb))
    }
  }

  test("matrix associativity") {
    forAll(matrices2, matrices2, matrices2) { (a, b, c) =>
      val m1 = (a * b) * c
      val m2 = a * (b * c)
      assert(m1 === (m2))
    }

    forAll(matrices3, matrices3, matrices3) { (a, b, c) =>
      val m1 = (a * b) * c
      val m2 = a * (b * c)
      assert(m1 === (m2))
    }

    forAll(matrices4, matrices4, matrices4) { (a, b, c) =>
      val m1 = (a * b) * c
      val m2 = a * (b * c)
      assert(m1 === (m2))
    }
  }

  test("matrix transpose") {
    forAll(matrices2, matrices2) { (a, b) =>
      assert((a * b).transposed() === (b.transposed() * a.transposed()))
    }

    forAll(matrices3, matrices3) { (a, b) =>
      assert((a * b).transposed() === (b.transposed() * a.transposed()))
    }

    forAll(matrices4, matrices4) { (a, b) =>
      assert((a * b).transposed() === (b.transposed() * a.transposed()))
    }
  }

  test("identity matrix") {
    implicit val doubleEquality: Equality[Double] = TolerantNumerics.tolerantDoubleEquality(0.000001)

    val id2 = Matrix2d().setIdentity()
    assert(id2 === (id2 * id2))
    assert(id2.det() === 1.0)

    val id3 = Matrix3d().setIdentity()
    assert(id3 === (id3 * id3))
    assert(id3.det() === 1.0)

    val id4 = Matrix4d().setIdentity()
    assert(id4 === (id4 * id4))
    assert(id4.det() === 1.0)
  }

  test("multiplication of determinants") {
    implicit val doubleEquality: Equality[Double] = TolerantNumerics.tolerantDoubleEquality(0.000001)
    forAll(matrices2, matrices2) { (m1, m2) =>
      assert((m1 * m2).det() === m1.det() * m2.det())
    }

    forAll(matrices3, matrices3) { (m1, m2) =>
      assert((m1 * m2).det() === m1.det() * m2.det())
    }

    forAll(matrices4, matrices4) { (m1, m2) =>
      assert((m1 * m2).det() === m1.det() * m2.det())
    }
  }

  test("transposed rotation is inverse rotation") {
    forAll(angleRadians) { angle =>
      val m = Matrix2d().set2dRotation(angle)
      val mTr = m.transposed()
      val mInv = m.inverted()
      assert(mTr === (mInv))
    }

    forAll(normalizedQuaternions) { q =>
      val m = Matrix3d() := q
      val mTr = m.transposed()
      val mInv = m.inverted()
      assert(mTr === (mInv))
    }

    forAll(normalizedQuaternions) { q =>
      val m = Matrix4d() := q
      val mTr = m.transposed()
      val mInv = m.inverted()
      assert(mTr === (mInv))
    }
  }

  test("matrix inversion") {
    implicit val eps: EqualityEps = EqualityEps(1e-12)
    
    forAll(matrices2) { m =>
      if (Math.abs(m.det()) > 0.000001) {
        val inverted = m.inverted()
        val id = Matrix2d().setIdentity()

        assert(id === (m * inverted), s"diff = ${id - (m * inverted)}")
        assert(id === (inverted * m), s"diff = ${id - (inverted * m)}")
      }
    }

    forAll(matrices3) { m =>
      if (Math.abs(m.det()) > 0.000001) {
        val inverted = m.inverted()
        val id = Matrix3d().setIdentity()

        assert(id === (m * inverted), s"diff = ${id - (m * inverted)}")
        assert(id === (inverted * m), s"diff = ${id - (inverted * m)}")
      }
    }

    forAll(matrices4) { m =>
      if (Math.abs(m.det()) > 0.000001) {
        val inverted = m.inverted()
        val id = Matrix4d().setIdentity()

        assert(id === (m * inverted), s"diff = ${id - (m * inverted)}")
        assert(id === (inverted * m), s"diff = ${id - (inverted * m)}")
      }
    }
  }

  test("2d rotation") {
    forAll(angleRadians) { angle =>
      val ma = Matrix3d().set2dRotation(angle)
      val mb = Matrix3d() := (Quaternion(angle, Vector3d(0, 0, 1)))
      val mc = Matrix3d() := Matrix2d().set2dRotation(angle)
      assert(ma === (mb))
      assert(ma === (mc))
    }
  }

  test("multiply lower dimension vector") {
    implicit val doubleEquality: Equality[Double] = TolerantNumerics.tolerantDoubleEquality(0.000001)
    forAll(vectors2InCube, matrices3) { (v, m) =>
      val v2 = m * v
      val v3 = m * Vector3d(v.x, v.y, 1.0)
      if (Math.abs(v3.z) > 0.000001) {
        assert(v2.x === v3.x / v3.z)
        assert(v2.y === v3.y / v3.z)
      }
    }

    forAll(vectors3InCube, matrices4) { (v, m) =>
      val v2 = m * v
      val v3 = m * Vector4d(v.x, v.y, v.z, 1.0)
      if (Math.abs(v3.z) > 0.000001) {
        assert(v2.x === v3.x / v3.w)
        assert(v2.y === v3.y / v3.w)
        assert(v2.z === v3.z / v3.w)
      }
    }
  }

  test("Matrix234d multiplication correspondence") {
    forAll(matrices2, matrices2) { (left, right) =>
      val r1 = Matrix3d() := (left * right)
      val r2 = (Matrix3d() := left) * (Matrix3d() := right)
      assert(r1 === (r2))
    }

    forAll(matrices3, matrices3) { (left, right) =>
      val r1 = Matrix4d() := (left * right)
      val r2 = (Matrix4d() := left) * (Matrix4d() := right)
      assert(r1 === (r2))
    }
  }

  test("translation") {
    forAll(vectors2InCube, vectors2InCube) { (tr, v) =>
      assert((Matrix3d().set2dTranslation(tr) * v) === (tr + v))
    }

    forAll(vectors3InCube, vectors3InCube) { (tr, v) =>
      assert((Matrix4d().setTranslation(tr) * v) === (tr + v))
    }
  }

  test("perspective camera") {
    val near = 0.1
    val far = 1.0
    val m = Matrix4d().setProjectionCamera(1.0, 0.5, near, far)

    assert((m * Vector3d(0.0, 0.0, near)) === (Vector3d(0.0, 0.0, -1.0)))
    assert((m * Vector3d(0.0, 0.0, far)) === (Vector3d(0.0, 0.0, 1.0)))
    assert((m * Vector3d(near, 0.0, near)) === (Vector3d(1.0, 0.0, -1.0)))
    assert((m * Vector3d(0.0, near * 0.5, near)) === (Vector3d(0.0, 1.0, -1.0)))
  }

  test("perspectiveZ10 camera") {
    val near = 0.1
    val far = 1.0
    val m = Matrix4d().setProjectionCameraZ10(1.0, 0.5, near, far)

    assert((m * Vector3d(0.0, 0.0, near)) === (Vector3d(0.0, 0.0, -1.0)))
    assert((m * Vector3d(0.0, 0.0, far)) === (Vector3d(0.0, 0.0, 0.0)))
    assert((m * Vector3d(near, 0.0, near)) === (Vector3d(1.0, 0.0, -1.0)))
    assert((m * Vector3d(0.0, near * 0.5, near)) === (Vector3d(0.0, 1.0, -1.0)))
  }

  test("perspectiveZ10 camera with infinity far") {
    val near = 0.1
    val farExample = 1000.0
    val m = Matrix4d().setProjectionCameraZ10(1.0, 0.5, near)

    assert((m * Vector3d(0.0, 0.0, near)) === (Vector3d(0.0, 0.0, -1.0)))
    assert((m * Vector3d(0.0, 0.0, farExample)) === (Vector3d(0.0, 0.0, -near / farExample)))
    assert((m * Vector3d(near, 0.0, near)) === (Vector3d(1.0, 0.0, -1.0)))
    assert((m * Vector3d(0.0, near * 0.5, near)) === (Vector3d(0.0, 1.0, -1.0)))
  }

  test("Matrix setLookAt with normalized perpendicular axis") {
    forAll(vectors3InCube, vectors3InCube) { (from, dest) =>
      val up = Vector3d(0, 1, 0)
      if (from.distance(dest) > 0.000001 && (dest - from).rejected(up).mag > 0.000001) {
        val m4 = Matrix4d().setLookAt(from, dest, up)
        val m4r = Matrix4d().setRotation(dest - from, up)
        val m3 = Matrix3d().setRotation(dest - from, up)
        assert(Matrix3d().setRotation(m4) === (m3))
        assert(Matrix3d().setRotation(m4r) === (m3))
        assert((m3 * m3.transposed()) === (Matrix3d().setIdentity()))
      }
    }
  }

  test("Matrix setLookAt") {
    {
      val m = Matrix4d().setLookAt(Vector3d(0, 0, 0), Vector3d(0, 0, 1), Vector3d(0, 1, 0))
      assert(m === (Matrix4d().setIdentity()))
    }

    {
      val m = Matrix4d().setLookAt(Vector3d(0, 0, 0), Vector3d(1, 0, 0), Vector3d(0, 1, 0))
      assert((m * Vector3d(1, 0, 0)) === (Vector3d(0, 0, 1)))
      assert((m * Vector3d(1, 0, 0)) === (Vector3d(0, 0, 1)))
    }

    val up = Vector3d(0, 1, 0)
    forAll(vectors3InCube, vectors3InCube) { (from, dest) =>
      if (from.distance(dest) > 0.000001 && (dest - from).rejected(up).mag > 0.000001) {
        val m4 = Matrix4d().setLookAt(from, dest, up)
        val m3 = Matrix3d().setRotation(dest - from, up)
        val m4r = Matrix4d().setRotation(dest - from, up)

        require(Matrix3d().setRotation(m4) === (m3))
        require(Matrix3d().setRotation(m4r) === (m3))
        require((m4 * from) === (Vector3d(0, 0, 0)))
        require((m4 * dest) === (Vector3d(0, 0, dest.distance(from))))
      }
    }
  }

  test("Matrix and scalar multiplication and division") {
    forAll(matrices2, double1.filter(math.abs(_) > 1e-9)) { (matrix, m) =>
      require((matrix * m) === (m * matrix))
      require((matrix / m) === (matrix * (1.0 / m)))
    }

    forAll(matrices3, double1.filter(math.abs(_) > 1e-9)) { (matrix, m) =>
      require((matrix * m) === (m * matrix))
      require((matrix / m) === (matrix * (1.0 / m)))
    }

    forAll(matrices4, double1.filter(math.abs(_) > 1e-9)) { (matrix, m) =>
      require((matrix * m) === (m * matrix))
      require((matrix / m) === (matrix * (1.0 / m)))
    }
  }
