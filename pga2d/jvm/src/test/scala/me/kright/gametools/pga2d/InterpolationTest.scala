package me.kright.gametools.pga2d

import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class InterpolationTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private def minNorm(a: Pga2dRotor, b: Pga2dRotor): Double =
    Math.min((a - b).norm, (a + b).norm)

  private def minNorm(a: Pga2dMotor, b: Pga2dMotor): Double =
    Math.min((a - b).norm, (a + b).norm)

  private def rotorForAngle(angle: Double): Pga2dRotor =
    Pga2dRotor(Math.cos(angle * 0.5), Math.sin(angle * 0.5))

  // this and b are considered too close to antipodal (geodesic ill-conditioned) when this test threshold is exceeded
  private val antipodalThreshold = -1.0 + 1e-6

  test("rotor slerp/nlerp endpoints") {
    forAll(Pga2dGenerators.normalizedRotors, Pga2dGenerators.normalizedRotors, MinSuccessful(300)) { (a, b) =>
      whenever(a.reverse.geometric(b).s > antipodalThreshold) {
        assert(minNorm(a.slerp(b, 0.0), a) < 1e-9)
        assert(minNorm(a.slerp(b, 1.0), b) < 1e-9)
        assert(minNorm(a.nlerp(b, 0.0), a) < 1e-9)
        assert(minNorm(a.nlerp(b, 1.0), b) < 1e-9)
      }
    }
  }

  test("motor slerp/nlerp endpoints") {
    forAll(Pga2dGenerators.normalizedMotors, Pga2dGenerators.normalizedMotors, MinSuccessful(300)) { (a, b) =>
      whenever(a.reverse.geometric(b).s > antipodalThreshold) {
        assert(minNorm(a.slerp(b, 0.0), a) < 1e-8)
        assert(minNorm(a.slerp(b, 1.0), b) < 1e-8)
        assert(minNorm(a.nlerp(b, 0.0), a) < 1e-9)
        assert(minNorm(a.nlerp(b, 1.0), b) < 1e-9)
      }
    }
  }

  test("rotor slerp stays normalized") {
    forAll(Pga2dGenerators.normalizedRotors, Pga2dGenerators.normalizedRotors, Pga2dGenerators.double1, MinSuccessful(300)) { (a, b, t) =>
      whenever(a.reverse.geometric(b).s > antipodalThreshold) {
        assert(Math.abs(a.slerp(b, t).norm - 1.0) < 1e-9)
      }
    }
  }

  test("motor slerp stays a unit motor") {
    forAll(Pga2dGenerators.normalizedMotors, Pga2dGenerators.normalizedMotors, Pga2dGenerators.double1, MinSuccessful(300)) { (a, b, t) =>
      whenever(a.reverse.geometric(b).s > antipodalThreshold) {
        val res = a.slerp(b, t)
        assert((res - res.renormalized).norm < 1e-8)
      }
    }
  }

  test("rotor slerp approximates nlerp for small angles") {
    val angles = Seq(1e-4, 1e-3, 5e-3, 1e-2)
    forAll(Pga2dGenerators.normalizedRotors, MinSuccessful(200)) { a =>
      for (angle <- angles) {
        val delta = rotorForAngle(angle)
        val b = a.geometric(delta)
        for (t <- Seq(0.0, 0.25, 0.5, 0.75, 1.0)) {
          val diff = minNorm(a.slerp(b, t), a.nlerp(b, t))
          assert(diff < angle, s"angle = $angle, t = $t, diff = $diff")
        }
      }
    }
  }

  test("motor slerp approximates nlerp for small angles") {
    val angles = Seq(1e-4, 1e-3, 5e-3, 1e-2)
    forAll(Pga2dGenerators.normalizedMotors, MinSuccessful(200)) { a =>
      for (angle <- angles) {
        val deltaRotor = rotorForAngle(angle)
        val deltaMotor = Pga2dTranslator.addVector(Pga2dVector(angle, 0)).geometric(deltaRotor)
        val b = a.geometric(deltaMotor)
        for (t <- Seq(0.0, 0.25, 0.5, 0.75, 1.0)) {
          val diff = minNorm(a.slerp(b, t), a.nlerp(b, t))
          assert(diff < angle, s"angle = $angle, t = $t, diff = $diff")
        }
      }
    }
  }

  // Pga2dRotor has no dependency on the pga3d module (and vice versa), so this checks 2d/3d consistency
  // against the same elementary closed form that Pga3dRotor.slerp reduces to when restricted to the xy-plane
  // (see Pga3dRotorTest / InterpolationTest in the pga3d module), rather than importing Pga3dRotor.
  test("rotor slerp matches the elementary circle interpolation") {
    forAll(Pga2dGenerators.double1, Pga2dGenerators.double1, Pga2dGenerators.double1, MinSuccessful(300)) { (angleA, angleB, t) =>
      val a = rotorForAngle(angleA * Math.PI)
      val b = rotorForAngle(angleB * Math.PI)
      whenever(a.reverse.geometric(b).s > antipodalThreshold) {
        // slerp always takes the shorter arc (Pga2dRotor.slerp round-trips through log() internally,
        // which flips to the positive-scalar representative), so the reference formula must apply
        // the same double-cover correction: negate b when the dot product is negative.
        val dot0 = a.s * b.s + a.xy * b.xy
        val sign = if (dot0 < 0.0) -1.0 else 1.0
        val cosOmega = Math.max(-1.0, Math.min(1.0, dot0 * sign))
        val omega = Math.acos(cosOmega)

        val expected =
          if (Math.sin(omega) < 1e-6) {
            Pga2dRotor(a.s * (1.0 - t) + sign * b.s * t, a.xy * (1.0 - t) + sign * b.xy * t).normalizedByNorm
          } else {
            val sinOmega = Math.sin(omega)
            val wa = Math.sin((1.0 - t) * omega) / sinOmega
            val wb = sign * Math.sin(t * omega) / sinOmega
            Pga2dRotor(a.s * wa + b.s * wb, a.xy * wa + b.xy * wb)
          }

        assert(minNorm(a.slerp(b, t), expected) < 1e-9)
      }
    }
  }
