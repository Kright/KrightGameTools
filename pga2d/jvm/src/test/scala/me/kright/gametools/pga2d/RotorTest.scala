package me.kright.gametools.pga2d

import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class RotorTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private val restoreEps = 1e-12

  private def rotorForAngle(angle: Double): Pga2dRotor =
    Pga2dRotor(Math.cos(angle * 0.5), Math.sin(angle * 0.5))

  test("rotation between vectors maps from to to") {
    val from = Pga2dVector(1, 2).normalizedByNorm
    val to = Pga2dVector(3, 4).normalizedByNorm
    val r = Pga2dRotor.rotation(from, to)
    assert((r.sandwich(from) - to).norm < 1e-15)
  }

  test("rotation between random vectors") {
    // near-opposite pairs are covered by the dedicated branch-cut tests below
    forAll(Pga2dGenerators.vectors.filter(_.norm > 1e-3), Pga2dGenerators.vectors.filter(_.norm > 1e-3), MinSuccessful(1000)) { (from, to) =>
      val f = from.normalizedByNorm
      val t = to.normalizedByNorm
      whenever(f.x * t.x + f.y * t.y > -0.999) {
        val r = Pga2dRotor.rotation(from, to)
        assert(Math.abs(r.norm - 1.0) < 1e-9)
        assert((r.sandwich(f) - t).norm < 1e-7)
      }
    }
  }

  test("rotation between random ideal lines") {
    forAll(Pga2dGenerators.lineIdeals.filter(_.norm > 1e-3), Pga2dGenerators.lineIdeals.filter(_.norm > 1e-3), MinSuccessful(1000)) { (from, to) =>
      val f = from.normalizedByNorm
      val t = to.normalizedByNorm
      whenever(f.x * t.x + f.y * t.y > -0.999) {
        val r = Pga2dRotor.rotation(from, to)
        assert(Math.abs(r.norm - 1.0) < 1e-9)
        assert((r.sandwich(f) - t).norm < 1e-7)
      }
    }
  }

  test("rotation for opposite vectors") {
    val from = Pga2dVector(1, 2).normalizedByNorm
    val to = -from
    val r = Pga2dRotor.rotation(from, to)
    assert(Math.abs(r.norm - 1.0) < 1e-12)
    assert((r.sandwich(from) - to).norm < 1e-12)
  }

  test("rotation for nearly opposite vectors") {
    // pins the atan2 branch and the first-order near-pi fallback of rotation():
    // no deviation from pi may be dropped, however small
    for (angle <- Seq(0.0, 1e-12, 1e-8, 1e-6, 1e-4, 1e-3, 0.01)) {
      val from = Pga2dVector(1, 0)
      val to = Pga2dVector(-Math.cos(angle), Math.sin(angle))
      val r = Pga2dRotor.rotation(from, to)
      assert((r.sandwich(from) - to).norm < 1e-10, s"angle = $angle")
    }
  }

  test("sandwich preserves vector norm") {
    forAll(Pga2dGenerators.normalizedRotors, Pga2dGenerators.vectors, MinSuccessful(1000)) { (r, v) =>
      assert(Math.abs(r.sandwich(v).norm - v.norm) < 1e-13)
    }
  }

  test("rotor composition is composition of rotations") {
    forAll(Pga2dGenerators.normalizedRotors, Pga2dGenerators.normalizedRotors, Pga2dGenerators.vectors, MinSuccessful(1000)) { (r1, r2, v) =>
      val once = r1.geometric(r2).sandwich(v)
      val twice = r1.sandwich(r2.sandwich(v))
      assert((once - twice).norm < 1e-13)
    }
  }

  test("restore rotor from axes") {
    forAll(Pga2dGenerators.normalizedRotors, MinSuccessful(1000)) { r =>
      val axisX = r.sandwich(Pga2dVector(1, 0))
      val axisY = r.sandwich(Pga2dVector(0, 1))
      val restored = Pga2dRotor.restore(axisX, axisY)

      val diff1 = (restored - r).normSquare
      val diff2 = (restored + r).normSquare
      assert(Math.min(diff1, diff2) < restoreEps, s"restored = $restored, r = $r")
    }
  }

  test("restore small rotation") {
    val r = rotorForAngle(0.1)
    val restored = Pga2dRotor.restore(r.sandwich(Pga2dVector(1, 0)), r.sandwich(Pga2dVector(0, 1)))
    assert((restored - r).norm < restoreEps || (restored + r).norm < restoreEps)
  }

  test("restore rotations near the branch cut") {
    // Math.PI exercises the 180-degree case, +-(PI - 0.01) sit just inside the cosT <= -0.9 branch
    for (angle <- Seq(0.0, 0.5, 1.0, 2.0, 2.5, Math.PI - 0.01, Math.PI, -1.0, -2.5, -Math.PI + 0.01)) {
      val r = rotorForAngle(angle)
      val restored = Pga2dRotor.restore(r.sandwich(Pga2dVector(1, 0)), r.sandwich(Pga2dVector(0, 1)))
      assert((restored - r).norm < restoreEps || (restored + r).norm < restoreEps, s"angle = $angle, restored = $restored, r = $r")
    }
  }

  test("normalizedByNorm gives norm 1") {
    forAll(Pga2dGenerators.rotors.filter(_.norm > 1e-9), MinSuccessful(1000)) { r =>
      assert(Math.abs(r.normalizedByNorm.norm - 1.0) < 1e-12)
    }
  }
