package me.kright.gametools.vector

import me.kright.gametools.vector.VectorMathGenerators.*
import com.github.kright.mathutil.EqualityEps
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class VectorTest extends AnyFunSuite with ScalaCheckPropertyChecks:
  implicit val eps: EqualityEps = EqualityEps(1e-12)

  test("arithmetic") {
    forAll(vectors2InCube, vectors2InCube) { (v1, v2) =>
      assert(v1 + v2 === Vector2d(v1.x + v2.x, v1.y + v2.y))
      assert(v1 - v2 === Vector2d(v1.x - v2.x, v1.y - v2.y))
      assert(v1 * v2 === Vector2d(v1.x * v2.x, v1.y * v2.y))
      if (Math.abs(v2.x) > 1e-12 && Math.abs(v2.y) > 1e-12) {
        assert(v1 / v2 === Vector2d(v1.x / v2.x, v1.y / v2.y))
      }
      assert(v1 * 2.0 === Vector2d(v1.x * 2.0, v1.y * 2.0))
      assert((v1 ^ 2.0) === Vector2d(v1.x * v1.x, v1.y * v1.y))
    }

    forAll(vectors3InCube, vectors3InCube) { (v1, v2) =>
      assert(v1 + v2 === Vector3d(v1.x + v2.x, v1.y + v2.y, v1.z + v2.z))
      assert(v1 - v2 === Vector3d(v1.x - v2.x, v1.y - v2.y, v1.z - v2.z))
      assert(v1 * v2 === Vector3d(v1.x * v2.x, v1.y * v2.y, v1.z * v2.z))
      if (Math.abs(v2.x) > 1e-12 && Math.abs(v2.y) > 1e-12 && Math.abs(v2.z) > 1e-12) {
        assert(v1 / v2 === Vector3d(v1.x / v2.x, v1.y / v2.y, v1.z / v2.z))
      }
      assert(v1 * 2.0 === Vector3d(v1.x * 2.0, v1.y * 2.0, v1.z * 2.0))
      assert((v1 ^ 2.0) === Vector3d(v1.x * v1.x, v1.y * v1.y, v1.z * v1.z))
    }

    forAll(vectors4InCube, vectors4InCube) { (v1, v2) =>
      assert(v1 + v2 === Vector4d(v1.x + v2.x, v1.y + v2.y, v1.z + v2.z, v1.w + v2.w))
      assert(v1 - v2 === Vector4d(v1.x - v2.x, v1.y - v2.y, v1.z - v2.z, v1.w - v2.w))
      assert(v1 * v2 === Vector4d(v1.x * v2.x, v1.y * v2.y, v1.z * v2.z, v1.w * v2.w))
      if (Math.abs(v2.x) > 1e-12 && Math.abs(v2.y) > 1e-12 && Math.abs(v2.z) > 1e-12 && Math.abs(v2.w) > 1e-12) {
        assert(v1 / v2 === Vector4d(v1.x / v2.x, v1.y / v2.y, v1.z / v2.z, v1.w / v2.w))
      }
      assert(v1 * 2.0 === Vector4d(v1.x * 2.0, v1.y * 2.0, v1.z * 2.0, v1.w * 2.0))
      assert((v1 ^ 2.0) === Vector4d(v1.x * v1.x, v1.y * v1.y, v1.z * v1.z, v1.w * v1.w))
    }
  }

  test("projected and unprojected") {
    forAll(vectors2InCube, vectors2InCube) { (v1, axis) =>
      if (axis.mag > 0.000001) {
        assert(v1 === (v1.projected(axis) + v1.rejected(axis)))
      }
    }

    forAll(vectors3InCube, vectors3InCube) { (v1, axis) =>
      if (axis.mag > 0.000001) {
        assert(v1 === (v1.projected(axis) + v1.rejected(axis)))
      }
    }

    forAll(vectors4InCube, vectors4InCube) { (v1, axis) =>
      if (axis.mag > 0.000001) {
        assert(v1 === (v1.projected(axis) + v1.rejected(axis)))
      }
    }
  }

  test("cross product") {
    assert(Vector3d(1.0, 0.0, 0.0).cross(Vector3d(0.0, 1.0, 0.0)) === Vector3d(0.0, 0.0, 1.0))
  }

  test("min, max, clamp") {
    val v1 = Vector3d(1, 2, 3)
    val v2 = Vector3d(3, 2, 1)
    assert(v1.min(v2) === Vector3d(1, 2, 1))
    assert(v1.max(v2) === Vector3d(3, 2, 3))
    assert(v1.clamp(Vector3d(1.5, 1.5, 1.5), Vector3d(2.5, 2.5, 2.5)) === Vector3d(1.5, 2, 2.5))

    val v4a = Vector4d(1, 2, 3, 4)
    val v4b = Vector4d(4, 3, 2, 1)
    assert(v4a.min(v4b) === Vector4d(1, 2, 2, 1))
    assert(v4a.max(v4b) === Vector4d(4, 3, 3, 4))
    assert(v4a.clamp(Vector4d(2, 2, 2, 2), Vector4d(3, 3, 3, 3)) === Vector4d(2, 2, 3, 3))
  }
