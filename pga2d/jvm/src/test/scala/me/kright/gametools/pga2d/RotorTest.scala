package me.kright.gametools.pga2d

import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import scala.collection.immutable.ArraySeq

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
    forAll(Pga2dGenerators.lineCentrals.filter(_.norm > 1e-3), Pga2dGenerators.lineCentrals.filter(_.norm > 1e-3), MinSuccessful(1000)) { (from, to) =>
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
    for (angle <- ArraySeq(0.0, 1e-12, 1e-8, 1e-6, 1e-4, 1e-3, 0.01)) {
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
    for (angle <- ArraySeq(0.0, 0.5, 1.0, 2.0, 2.5, Math.PI - 0.01, Math.PI, -1.0, -2.5, -Math.PI + 0.01)) {
      val r = rotorForAngle(angle)
      val restored = Pga2dRotor.restore(r.sandwich(Pga2dVector(1, 0)), r.sandwich(Pga2dVector(0, 1)))
      assert((restored - r).norm < restoreEps || (restored + r).norm < restoreEps, s"angle = $angle, restored = $restored, r = $r")
    }
  }

  test("log returns the half-angle and exp is its inverse") {
    for (h <- ArraySeq(0.0, 1e-300, 1e-20, 1e-15, 1e-8, 0.1, 1.0, Math.PI / 2 - 1e-9, -0.3, -1.5)) {
      val r = Pga2dRotor.exp(h)
      assert(Math.abs(r.log() - h) <= 1e-15 * Math.abs(h), s"h = $h, log = ${r.log()}")
    }

    forAll(Pga2dGenerators.normalizedRotors, MinSuccessful(1000)) { r =>
      val restored = Pga2dRotor.exp(r.log())
      assert(Math.min((restored - r).normSquare, (restored + r).normSquare) < 1e-28, s"r = $r")
    }
  }

  test("log takes the principal branch for negative s") {
    val r = Pga2dRotor.exp(2.0) // s = cos(2) < 0, the principal generator is (2 - pi)
    assert(Math.abs(r.log() - (2.0 - Math.PI)) <= 1e-15, s"log = ${r.log()}")
  }

  test("axisX and axisY match the sandwiched basis vectors") {
    forAll(Pga2dGenerators.normalizedRotors, MinSuccessful(1000)) { r =>
      assert((r.axisX - r.sandwich(Pga2dVector(1, 0))).norm < 1e-15, s"axisX of $r")
      assert((r.axisY - r.sandwich(Pga2dVector(0, 1))).norm < 1e-15, s"axisY of $r")
    }
  }

  test("restore is the inverse of axisX/axisY") {
    forAll(Pga2dGenerators.normalizedRotors, MinSuccessful(1000)) { r =>
      val restored = Pga2dRotor.restore(r.axisX, r.axisY)
      assert(Math.min((restored - r).normSquare, (restored + r).normSquare) < restoreEps, s"r = $r")
    }
  }

  test("motor axes come from its rotor part") {
    forAll(Pga2dGenerators.normalizedRotors, Pga2dGenerators.vectors) { (r, v) =>
      val motor = Pga2dTranslator.addVector(v).geometric(r)
      assert((motor.axisX - r.axisX).norm < 1e-15, s"axisX of $motor")
      assert((motor.axisY - r.axisY).norm < 1e-15, s"axisY of $motor")
    }
  }

  test("normalizedByNorm gives norm 1") {
    forAll(Pga2dGenerators.rotors.filter(_.norm > 1e-9), MinSuccessful(1000)) { r =>
      assert(Math.abs(r.normalizedByNorm.norm - 1.0) < 1e-12)
    }
  }
