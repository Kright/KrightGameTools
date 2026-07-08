package me.kright.gametools.pga3d

import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class InterpolationTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private def minNorm(a: Pga3dRotor, b: Pga3dRotor): Double =
    Math.min((a - b).norm, (a + b).norm)

  private def minNorm(a: Pga3dMotor, b: Pga3dMotor): Double =
    Math.min((a - b).norm, (a + b).norm)

  private def rotorForAngle(angle: Double): Pga3dRotor =
    Pga3dRotor(Math.cos(angle * 0.5), Math.sin(angle * 0.5), 0, 0)

  // this and b are considered too close to antipodal (geodesic ill-conditioned) when this test threshold is exceeded
  private val antipodalThreshold = -1.0 + 1e-6

  test("rotor slerp/nlerp endpoints") {
    forAll(Pga3dGenerators.normalizedRotors, Pga3dGenerators.normalizedRotors, MinSuccessful(300)) { (a, b) =>
      whenever(a.reverse.geometric(b).s > antipodalThreshold) {
        assert(minNorm(a.slerp(b, 0.0), a) < 1e-9)
        assert(minNorm(a.slerp(b, 1.0), b) < 1e-9)
        assert(minNorm(a.nlerp(b, 0.0), a) < 1e-9)
        assert(minNorm(a.nlerp(b, 1.0), b) < 1e-9)
      }
    }
  }

  test("motor slerp/nlerp endpoints") {
    forAll(Pga3dGenerators.normalizedMotors, Pga3dGenerators.normalizedMotors, MinSuccessful(300)) { (a, b) =>
      whenever(a.reverse.geometric(b).s > antipodalThreshold) {
        assert(minNorm(a.slerp(b, 0.0), a) < 1e-8)
        assert(minNorm(a.slerp(b, 1.0), b) < 1e-8)
        assert(minNorm(a.nlerp(b, 0.0), a) < 1e-9)
        assert(minNorm(a.nlerp(b, 1.0), b) < 1e-9)
      }
    }
  }

  test("rotor slerp stays normalized") {
    forAll(Pga3dGenerators.normalizedRotors, Pga3dGenerators.normalizedRotors, Pga3dGenerators.double1, MinSuccessful(300)) { (a, b, t) =>
      whenever(a.reverse.geometric(b).s > antipodalThreshold) {
        assert(Math.abs(a.slerp(b, t).norm - 1.0) < 1e-9)
      }
    }
  }

  test("motor slerp stays a unit motor") {
    forAll(Pga3dGenerators.normalizedMotors, Pga3dGenerators.normalizedMotors, Pga3dGenerators.double1, MinSuccessful(300)) { (a, b, t) =>
      whenever(a.reverse.geometric(b).s > antipodalThreshold) {
        val res = a.slerp(b, t)
        assert((res - res.renormalized).norm < 1e-8)
      }
    }
  }

  test("rotor slerp approximates nlerp for small angles") {
    val angles = Seq(1e-4, 1e-3, 5e-3, 1e-2)
    forAll(Pga3dGenerators.normalizedRotors, MinSuccessful(200)) { a =>
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
    forAll(Pga3dGenerators.normalizedMotors, MinSuccessful(200)) { a =>
      for (angle <- angles) {
        val deltaRotor = rotorForAngle(angle)
        val deltaMotor = Pga3dTranslator.addVector(Pga3dVector(angle, 0, 0)).geometric(deltaRotor)
        val b = a.geometric(deltaMotor)
        for (t <- Seq(0.0, 0.25, 0.5, 0.75, 1.0)) {
          val diff = minNorm(a.slerp(b, t), a.nlerp(b, t))
          assert(diff < angle, s"angle = $angle, t = $t, diff = $diff")
        }
      }
    }
  }

  // Pga3dRotor has no dependency on the pga2d module (and vice versa), so this checks 2d/3d consistency
  // against the same elementary closed form that Pga2dRotor.slerp reduces to, rather than importing Pga2dRotor.
  test("rotor slerp restricted to the xy-plane matches the elementary circle interpolation") {
    forAll(Pga3dGenerators.double1, Pga3dGenerators.double1, Pga3dGenerators.double1, MinSuccessful(300)) { (angleA, angleB, t) =>
      val a = rotorForAngle(angleA * Math.PI)
      val b = rotorForAngle(angleB * Math.PI)
      whenever(a.reverse.geometric(b).s > antipodalThreshold) {
        // slerp always takes the shorter arc (Pga3dRotor.log() flips to the positive-scalar
        // representative internally), so the reference formula must apply the same double-cover
        // correction: negate b when the dot product is negative.
        val dot0 = a.s * b.s + a.xy * b.xy
        val sign = if (dot0 < 0.0) -1.0 else 1.0
        val cosOmega = Math.max(-1.0, Math.min(1.0, dot0 * sign))
        val omega = Math.acos(cosOmega)

        val expected =
          if (Math.sin(omega) < 1e-6) {
            Pga3dRotor(a.s * (1.0 - t) + sign * b.s * t, a.xy * (1.0 - t) + sign * b.xy * t, 0, 0).normalizedByNorm
          } else {
            val sinOmega = Math.sin(omega)
            val wa = Math.sin((1.0 - t) * omega) / sinOmega
            val wb = sign * Math.sin(t * omega) / sinOmega
            Pga3dRotor(a.s * wa + b.s * wb, a.xy * wa + b.xy * wb, 0, 0)
          }

        val res = a.slerp(b, t)
        assert(res.xz == 0.0 && res.yz == 0.0)
        assert(minNorm(res, expected) < 1e-9)
      }
    }
  }

  // Pga3dRotor only: verifies slerp() against the classic quaternion slerp identity
  // (treating (s, xy, xz, yz) as the quaternion (w, x, y, z)).
  test("rotor slerp matches the classic quaternion slerp identity") {
    forAll(Pga3dGenerators.normalizedRotors, Pga3dGenerators.normalizedRotors, Pga3dGenerators.double1, MinSuccessful(500)) { (a, b, t) =>
      val dot0 = a.s * b.s + a.xy * b.xy + a.xz * b.xz + a.yz * b.yz
      val sign = if (dot0 < 0.0) -1.0 else 1.0
      val dot = dot0 * sign

      whenever(dot < 1.0 - 1e-6) {
        val omega = Math.acos(Math.max(-1.0, Math.min(1.0, dot)))

        whenever(Math.sin(omega) > 1e-6) {
          val sinOmega = Math.sin(omega)
          val wa = Math.sin((1.0 - t) * omega) / sinOmega
          val wb = sign * Math.sin(t * omega) / sinOmega
          val expected = Pga3dRotor(
            a.s * wa + b.s * wb,
            a.xy * wa + b.xy * wb,
            a.xz * wa + b.xz * wb,
            a.yz * wa + b.yz * wb,
          )
          val diff = minNorm(a.slerp(b, t), expected)
          assert(diff < 1e-9, s"a = $a, b = $b, t = $t, diff = $diff")
        }
      }
    }
  }
