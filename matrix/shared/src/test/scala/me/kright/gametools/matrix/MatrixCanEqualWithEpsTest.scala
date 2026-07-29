package me.kright.gametools.matrix

import me.kright.gametools.mathutil.CanEqualWithEps
import org.scalatest.funsuite.AnyFunSuiteLike

class MatrixCanEqualWithEpsTest extends AnyFunSuiteLike:

  private def genericEquals[T: CanEqualWithEps](a: T, b: T, eps: Double): Boolean =
    a.equalsWithEps(b, eps)

  test("Matrix instance compares elementwise and rejects size mismatch") {
    val m = Matrix.fromValues(2, 2)(1.0, 2.0, 3.0, 4.0)
    val closeEnough = Matrix.fromValues(2, 2)(1.05, 2.0, 3.0, 4.0)
    val tooFar = Matrix.fromValues(2, 2)(1.5, 2.0, 3.0, 4.0)
    val otherShape = Matrix.fromValues(1, 4)(1.0, 2.0, 3.0, 4.0)

    assert(m.equalsWithEps(closeEnough, eps = 0.1))
    assert(!m.equalsWithEps(tooFar, eps = 0.1))
    assert(!m.equalsWithEps(otherShape, eps = 0.1))
    assert(genericEquals(m, closeEnough, eps = 0.1))
  }
