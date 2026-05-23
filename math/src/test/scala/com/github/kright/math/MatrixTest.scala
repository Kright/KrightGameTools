package com.github.kright.math

import com.github.kright.math.MathGenerators.*
import com.github.kright.math.VectorMathGenerators.*
import com.github.kright.matrix.{Matrix, Matrix2d, Matrix3d, Matrix4d}
import org.scalactic.{Equality, TolerantNumerics}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

import scala.util.chaining.scalaUtilChainingOps

def column(v: Vector3d): Matrix =
  Matrix(3, 1, Array(v.x, v.y, v.z))

def vector3d(m: Matrix): Vector3d = {
  require(m.h == 3 && m.w == 1, "Matrix must be 3x1")
  Vector3d(m(0, 0), m(1, 0), m(2, 0))
}

def mult(m: Matrix, v: Vector3d): Vector3d = {
  vector3d(m * column(v))
}

extension (m: Matrix)
  def *(v: Vector3d): Vector3d = vector3d(m * column(v))

class MatrixTest extends AnyFunSuite with ScalaCheckPropertyChecks:
  private implicit val eps: EqualityEps = EqualityEps(1e-12)



  test("matrix and quaternion multiplication consistency") {
    forAll(normalizedQuaternions, vectors3InCube) { case (q, v) =>
      val mat3 = q.toMatrix
      assert(mat3 * v === (q * v))

      val oX = q.oX
      val oY = q.oY
      val oZ = q.oZ

      mat3 := Matrix3d(Array(
        oX.x, oX.y, oX.z,
        oY.x, oY.y, oY.z,
        oZ.x, oZ.y, oZ.z
      ))
      assert((mat3 * v) === (q * v))
    }
  }

  test("matrix and quaternion same multiplication order") {
    forAll(normalizedQuaternions, normalizedQuaternions) { case (q1, q2) =>
      val ma = (q1 * q2).toMatrix
      val mb = q1.toMatrix * q2.toMatrix
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
      assert((a * b).transposedCopy() === (b.transposedCopy() * a.transposedCopy()))
    }

    forAll(matrices3, matrices3) { (a, b) =>
      assert((a * b).transposedCopy() === (b.transposedCopy() * a.transposedCopy()))
    }

    forAll(matrices4, matrices4) { (a, b) =>
      assert((a * b).transposedCopy() === (b.transposedCopy() * a.transposedCopy()))
    }
  }

  test("identity matrix") {
    implicit val doubleEquality: Equality[Double] = TolerantNumerics.tolerantDoubleEquality(0.000001)

    val id2 = Matrix2d.id
    assert(id2 === (id2 * id2))
    assert(id2.det() === 1.0)

    val id3 = Matrix3d.id
    assert(id3 === (id3 * id3))
    assert(id3.det() === 1.0)

    val id4 = Matrix4d.id
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
    forAll(normalizedQuaternions) { q =>
      val m = q.toMatrix
      val mTr = m.transposedCopy()
      val mInv = m.inverted()
      assert(mTr === (mInv))
    }

    forAll(normalizedQuaternions) { q =>
      val m = Matrix4d.id
      m.view(0 until 3, 0 until 3) := q.toMatrix
      val mTr = m.transposedCopy()
      val mInv = m.inverted()
      assert(mTr === (mInv))
    }
  }

  test("matrix inversion") {
    implicit val eps: EqualityEps = EqualityEps(1e-12)

    forAll(matrices2) { m =>
      if (Math.abs(m.det()) > 0.000001) {
        val inverted = m.inverted()
        val id = Matrix2d.id

        assert(id === (m * inverted), s"diff = ${id - (m * inverted)}")
        assert(id === (inverted * m), s"diff = ${id - (inverted * m)}")
      }
    }

    forAll(matrices3) { m =>
      if (Math.abs(m.det()) > 0.000001) {
        val inverted = m.inverted()
        val id = Matrix3d.id

        assert(id === (m * inverted), s"diff = ${id - (m * inverted)}")
        assert(id === (inverted * m), s"diff = ${id - (inverted * m)}")
      }
    }

    forAll(matrices4) { m =>
      if (Math.abs(m.det()) > 0.000001) {
        val inverted = m.inverted()
        val id = Matrix4d.id

        assert(id === (m * inverted), s"diff = ${id - (m * inverted)}")
        assert(id === (inverted * m), s"diff = ${id - (inverted * m)}")
      }
    }
  }

  test("Matrix234d multiplication correspondence") {
    forAll(matrices2, matrices2) { (left, right) =>
      val r1 = Matrix3d.zero
      r1.view(0 until 2, 0 until 2) := (left * right)

      val r2 = Matrix3d.zero.tap(_.view(0 until 2, 0 until 2) := left) * Matrix3d.zero.tap(_.view(0 until 2, 0 until 2) := right)
      assert(r1 === r2)
    }

    forAll(matrices3, matrices3) { (left, right) =>
      val r1 = Matrix4d.zero.tap(_.view(0 until 3, 0 until 3) := (left * right))
      val r2 = (Matrix4d.zero.tap(_.view(0 until 3, 0 until 3) := left)) * (Matrix4d.zero.tap(_.view(0 until 3, 0 until 3) := right))
      assert(r1 === r2)
    }
  }
