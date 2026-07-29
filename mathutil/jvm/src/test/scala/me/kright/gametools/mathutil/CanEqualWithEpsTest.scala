package me.kright.gametools.mathutil

import org.scalatest.funsuite.AnyFunSuiteLike

object CanEqualWithEpsTest:
  case class Vec(x: Double, y: Double, z: Double) derives CanEqualWithEps

  case class Segment(a: Vec, b: Vec) derives CanEqualWithEps

  def genericEquals[T: CanEqualWithEps](a: T, b: T, eps: Double): Boolean =
    a.equalsWithEps(b, eps)

class CanEqualWithEpsTest extends AnyFunSuiteLike:

  import CanEqualWithEpsTest.*

  test("chebyshev semantics: every component must be within eps") {
    val v = Vec(1.0, 2.0, 3.0)
    assert(v.equalsWithEps(Vec(1.1, 2.1, 3.1), eps = 0.1 + 1e-9))
    // one component out of tolerance is enough to fail, even if the others match exactly
    assert(!v.equalsWithEps(Vec(1.0, 2.0, 3.2), eps = 0.1))
    assert(v.equalsWithEps(v, eps = 0.0))
  }

  test("nested case classes are compared recursively") {
    val s = Segment(Vec(1.0, 2.0, 3.0), Vec(4.0, 5.0, 6.0))
    assert(s.equalsWithEps(Segment(Vec(1.0, 2.0, 3.0), Vec(4.0, 5.0, 6.05)), eps = 0.1))
    assert(!s.equalsWithEps(Segment(Vec(1.0, 2.0, 3.0), Vec(4.0, 5.0, 6.5)), eps = 0.1))
  }

  test("equal infinities are equal, NaN is not equal to anything") {
    val inf = Vec(Double.PositiveInfinity, 0.0, 0.0)
    assert(inf.equalsWithEps(inf, eps = 1e-9))
    assert(!inf.equalsWithEps(Vec(Double.NegativeInfinity, 0.0, 0.0), eps = 1e-9))

    val nan = Vec(Double.NaN, 0.0, 0.0)
    assert(!nan.equalsWithEps(nan, eps = 1e-9))
    assert(!nan.equalsWithEps(Vec(0.0, 0.0, 0.0), eps = Double.PositiveInfinity))
  }

  test("Double instance and generic context-bound usage") {
    // the Double instance lives in the CanEqualWithEps companion: unlike derived instances
    // (found through the companion of the receiver), direct calls on Double need this import
    import CanEqualWithEps.given
    assert(1.0.equalsWithEps(1.05, eps = 0.1))
    assert(!1.0.equalsWithEps(1.5, eps = 0.1))
    assert(genericEquals(1.0, 1.05, eps = 0.1))
    assert(!genericEquals(Vec(1.0, 2.0, 3.0), Vec(1.5, 2.0, 3.0), eps = 0.1))
  }
