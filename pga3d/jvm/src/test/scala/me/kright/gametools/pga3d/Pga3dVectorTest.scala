package me.kright.gametools.pga3d

import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class Pga3dVectorTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private val x = Pga3dVector(1, 0, 0)
  private val y = Pga3dVector(0, 1, 0)
  private val z = Pga3dVector(0, 0, 1)

  test("crossRightHanded and crossLeftHanded of basis vectors") {
    assert(Pga3dVector.crossRightHanded(x, y) == z)
    assert(Pga3dVector.crossRightHanded(y, z) == x)
    assert(Pga3dVector.crossRightHanded(z, x) == y)

    assert(Pga3dVector.crossLeftHanded(x, y) == -z)
    assert(Pga3dVector.crossLeftHanded(y, z) == -x)
    assert(Pga3dVector.crossLeftHanded(z, x) == -y)
  }

  test("cross is the reinterpreted join") {
    forAll(Pga3dGenerators.vectors, Pga3dGenerators.vectors) { (a, b) =>
      val m = a v b
      assert(Pga3dVector.crossRightHanded(a, b) == Pga3dVector(m.wx, m.wy, m.wz))
      assert(Pga3dVector.crossLeftHanded(a, b) == -Pga3dVector.crossRightHanded(a, b))
    }
  }

  test("cross is orthogonal to the arguments") {
    forAll(Pga3dGenerators.vectors, Pga3dGenerators.vectors) { (a, b) =>
      val c = Pga3dVector.crossRightHanded(a, b)
      assert(Math.abs(c antiDotI a) <= 1e-14)
      assert(Math.abs(c antiDotI b) <= 1e-14)
    }
  }
