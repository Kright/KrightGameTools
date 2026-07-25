package me.kright.gametools.pga3d

import org.scalatest.funsuite.AnyFunSuiteLike

/**
 * Corner-case precision tests for the exp/log family of pga3d: bivector/bulk/weight exp,
 * motor/rotor/translator log, and the 1e-5 series/sin branch thresholds inside them.
 *
 * Assertions are RELATIVE wherever the quantity has a scale, so tiny inputs
 * (1e-10 .. 1e-300) must survive round trips to near machine precision instead of
 * passing trivially under an absolute epsilon.
 *
 * Note on the sinMinusCosDivLen2 / log-c series branches: below the 1e-5 threshold their
 * contribution to any motor/bivector component is second-order small (~x^2/3 of the
 * component), so an end-to-end double test cannot resolve the series coefficients there —
 * those are proven by the derivations in the generated comments. What IS observable, and
 * what these tests pin down, is continuity across the branch threshold, sin/cos accuracy
 * of the bulk part, exactness of the pure-translation paths, and agreement of a screw
 * motion with its independently computed rotor*translator decomposition.
 */
class PrecisionTest extends AnyFunSuiteLike:

  /** relative difference; 0.0 for an exact match (including 0.0 vs -0.0) */
  private def relDiff(actual: Double, expected: Double): Double =
    if (actual == expected) 0.0
    else Math.abs(actual - expected) / Math.max(Math.abs(actual), Math.abs(expected))

  private def assertRel(actual: Double, expected: Double, eps: Double, clue: => String): Unit = {
    val diff = relDiff(actual, expected)
    assert(diff <= eps, s"$clue: actual = $actual, expected = $expected, relDiff = $diff")
  }

  test("bivector exp/log round trip is relatively precise for any half-angle") {
    // half-angles: zero, denormal-adjacent, tiny, both sides of the 1e-5 series/sin branch
    // threshold of exp() and log(), moderate, and close to pi/2 (where motor.s crosses 0)
    val halfAngles = Seq(
      0.0, 1e-300, 1e-100, 1e-50, 1e-20, 1e-10, 1e-6,
      9.9e-6, 1.01e-5, 2e-5, 1e-4, 1e-3, 1e-2, 0.1, 1.0, 1.5, Math.PI / 2 - 1e-6)
    // (2, -3, 6)/7 is a unit direction exercising all three bulk components
    val (dx, dy, dz) = (2.0 / 7, -3.0 / 7, 6.0 / 7)
    // exp/log mix the weight components through triple products of the same scale, so
    // same-scale shifts must be preserved with relative precision of that scale; the shift
    // (1, -2, 0.5) makes bulk*weight (and so the motor i and the log c term) non-zero
    val shifts = Seq((0.0, 0.0, 0.0), (1.0, -2.0, 0.5), (1e-10, 2e-10, -0.5e-10), (1e10, -2e10, 0.5e10))

    for (h <- halfAngles; sign <- Seq(1.0, -1.0); (sx, sy, sz) <- shifts) {
      val b = Pga3dBivector(
        wx = sx, wy = sy, wz = sz,
        xy = sign * h * dx, xz = sign * h * dy, yz = sign * h * dz)
      val restored = b.exp().log()

      // the bulk parts of exp and log are pure functions of the bulk, per-component relative
      assertRel(restored.xy, b.xy, 1e-14, s"xy of $b")
      assertRel(restored.xz, b.xz, 1e-14, s"xz of $b")
      assertRel(restored.yz, b.yz, 1e-14, s"yz of $b")

      // the weight components mix with each other (scale-preserving), so they are compared
      // relative to the common weight scale
      val weightScale = Math.max(Math.abs(sx), Math.max(Math.abs(sy), Math.abs(sz)))
      if (weightScale == 0.0) {
        assert(restored.wx == 0.0 && restored.wy == 0.0 && restored.wz == 0.0, s"weight of $b")
      } else {
        assert(Math.abs(restored.wx - b.wx) <= 1e-14 * weightScale, s"wx of $b: ${restored.wx} vs ${b.wx}")
        assert(Math.abs(restored.wy - b.wy) <= 1e-14 * weightScale, s"wy of $b: ${restored.wy} vs ${b.wy}")
        assert(Math.abs(restored.wz - b.wz) <= 1e-14 * weightScale, s"wz of $b: ${restored.wz} vs ${b.wz}")
      }
    }
  }

  test("rotor exp/log round trip is relatively precise for any half-angle") {
    val halfAngles = Seq(
      0.0, 1e-300, 1e-100, 1e-50, 1e-20, 1e-10, 1e-6,
      9.9e-6, 1.01e-5, 2e-5, 1e-4, 1e-3, 1e-2, 0.1, 1.0, 1.5, Math.PI / 2 - 1e-6)
    val (dx, dy, dz) = (2.0 / 7, -3.0 / 7, 6.0 / 7)

    for (h <- halfAngles; sign <- Seq(1.0, -1.0)) {
      val b = Pga3dBivectorBulk(xy = sign * h * dx, xz = sign * h * dy, yz = sign * h * dz)
      val restored = b.exp().log()
      assertRel(restored.xy, b.xy, 1e-14, s"xy of $b")
      assertRel(restored.xz, b.xz, 1e-14, s"xz of $b")
      assertRel(restored.yz, b.yz, 1e-14, s"yz of $b")
    }
  }

  test("exp sin(len)/len is continuous and accurate across the 1e-5 branch threshold") {
    for (w <- Seq(1e-300, 1e-20, 1e-7, 1e-6, 5e-6, 9.999999e-6, 1.0000001e-5, 2e-5, 1e-4, 1e-3)) {
      val rotor = Pga3dBivectorBulk(xy = w).exp()
      assertRel(rotor.xy, Math.sin(w), 5e-16, s"rotor sin of half-angle $w")
      assertRel(rotor.s, Math.cos(w), 5e-16, s"rotor cos of half-angle $w")

      val motor = Pga3dBivector(xy = w).exp()
      assertRel(motor.xy, Math.sin(w), 5e-16, s"motor sin of half-angle $w")
      assertRel(motor.s, Math.cos(w), 5e-16, s"motor cos of half-angle $w")
    }
  }

  test("screw motion exp matches its commuting rotor*translator decomposition across the threshold") {
    // B = h*e_xy + d*e_wz: rotation in the xy plane and translation along its z axis.
    // The two parts commute, so exp(B) must equal exp(bulk) * exp(weight), which goes
    // through the rotor and translator code paths and never touches sinMinusCosDivLen2.
    // This checks the full bivector exp (including the i component and the second-order
    // term) on both sides of the 1e-5 branch threshold.
    for (h <- Seq(1e-7, 1e-6, 9.9e-6, 9.999999e-6, 1.0000001e-5, 2e-5, 1e-4, 1e-3, 0.1);
         d <- Seq(0.0, 1e-10, 1.0, 1e10)) {
      val direct = Pga3dBivector(wz = d, xy = h).exp()
      val expected = Pga3dBivectorBulk(xy = h).exp().geometric(Pga3dBivectorWeight(wz = d).exp())

      assertRel(direct.s, expected.s, 5e-15, s"s for h = $h, d = $d")
      assertRel(direct.wx, expected.wx, 5e-15, s"wx for h = $h, d = $d")
      assertRel(direct.wy, expected.wy, 5e-15, s"wy for h = $h, d = $d")
      assertRel(direct.wz, expected.wz, 5e-15, s"wz for h = $h, d = $d")
      assertRel(direct.xy, expected.xy, 5e-15, s"xy for h = $h, d = $d")
      assertRel(direct.xz, expected.xz, 5e-15, s"xz for h = $h, d = $d")
      assertRel(direct.yz, expected.yz, 5e-15, s"yz for h = $h, d = $d")
      assertRel(direct.i, expected.i, 5e-15, s"i for h = $h, d = $d")
    }
  }

  test("exp(t) matches (b * t).exp()") {
    val bivectors = Seq(
      Pga3dBivector(0.2, 0.1, -0.3, 0.3, -0.4, 0.5),
      Pga3dBivector(1e-100, -2e-100, 1e-100, 2e-100, 1e-100, -1e-100),
      Pga3dBivector(wx = 1.0, wy = -2.0, wz = 0.5),
      Pga3dBivector(xy = 0.3, xz = -0.4, yz = 0.5))
    // t values keeping bulkNorm * t moderate: at huge angles the two sides legitimately
    // differ through the ~ulp difference of bulkNorm * t vs (b * t).bulkNorm before cos
    for (t <- Seq(0.0, 1e-300, 1e-100, 1e-10, 0.5, 1.0, 2.0); b <- bivectors) {
      val viaT = b.exp(t)
      val viaScale = (b * t).exp()
      assertRel(viaT.s, viaScale.s, 1e-14, s"s for b = $b, t = $t")
      assertRel(viaT.wx, viaScale.wx, 1e-14, s"wx for b = $b, t = $t")
      assertRel(viaT.wy, viaScale.wy, 1e-14, s"wy for b = $b, t = $t")
      assertRel(viaT.wz, viaScale.wz, 1e-14, s"wz for b = $b, t = $t")
      assertRel(viaT.xy, viaScale.xy, 1e-14, s"xy for b = $b, t = $t")
      assertRel(viaT.xz, viaScale.xz, 1e-14, s"xz for b = $b, t = $t")
      assertRel(viaT.yz, viaScale.yz, 1e-14, s"yz for b = $b, t = $t")
      assertRel(viaT.i, viaScale.i, 1e-14, s"i for b = $b, t = $t")
    }

    // extreme t is safe when the angle stays tiny; components are kept at 1e-150 so that
    // the pairwise products inside the i component (~1e-300) stay in the normal range —
    // at 1e-200 they underflow to zero in the t-factored form of exp(t)
    val tiny = Pga3dBivector(1e-150, -1e-150, 1e-150, 2e-150, 1e-150, -1e-150)
    for (t <- Seq(1e10, 1e100)) {
      val viaT = tiny.exp(t)
      val viaScale = (tiny * t).exp()
      assertRel(viaT.wx, viaScale.wx, 1e-14, s"wx for tiny bivector, t = $t")
      assertRel(viaT.xy, viaScale.xy, 1e-14, s"xy for tiny bivector, t = $t")
      assertRel(viaT.i, viaScale.i, 1e-14, s"i for tiny bivector, t = $t")
    }
  }

  test("bivector weight exp and translator log round trips are exact at any magnitude") {
    for (m <- Seq(0.0, 1e-300, 1e-100, 1e-10, 1.0, 1e10, 1e100, 1e300);
         (x, y, z) <- Seq((m, 0.0, 0.0), (0.0, -m, m), (m, -m, 0.5 * m), (m, 1.0, -m))) {
      val v = Pga3dBivectorWeight(x, y, z)
      assert(v.exp().log() == v, s"v = $v")
      val tr = Pga3dTranslator(x, y, z)
      assert(tr.log().exp() == tr, s"tr = $tr")
    }
  }

  test("motor log is exact for pure translation motors") {
    for (m <- Seq(0.0, 1e-300, 1e-100, 1e-10, 1.0, 1e10, 1e100)) {
      val motor = Pga3dMotor(s = 1.0, wx = m, wy = -m, wz = 0.5 * m)
      val expected = Pga3dBivector(wx = m, wy = -m, wz = 0.5 * m)
      assert(motor.log() == expected, s"motor = $motor")
    }
  }

  test("motor and rotor log angle recovery is continuous across the 1e-5 branch threshold") {
    // log must return the half-angle w through atan2 on both sides of the
    // b = angle/sin(angle) series/sqrt branch split
    for (w <- Seq(9.99e-6, 9.999999e-6, 1.0000001e-5, 1.001e-5)) {
      val motor = Pga3dMotor(s = Math.cos(w), xy = Math.sin(w))
      assertRel(motor.log().xy, w, 1e-15, s"motor recovered half-angle for w = $w")

      val rotor = Pga3dRotor(s = Math.cos(w), xy = Math.sin(w))
      assertRel(rotor.log().xy, w, 1e-15, s"rotor recovered half-angle for w = $w")
    }
  }

  test("motor log/exp round trip at corner half-angles and translations") {
    for (h <- Seq(0.0, 1e-10, 1e-4, 0.5, Math.PI / 2 - 1e-8, Math.PI / 2 + 0.3, 3.0);
         v <- Seq(Pga3dVector(0, 0, 0), Pga3dVector(1e-20, 1e-20, -1e-20), Pga3dVector(3, -4, 5))) {
      val rotor = Pga3dBivectorBulk(xy = h).exp()
      val motor = Pga3dTranslator.addVector(v).geometric(rotor)
      val restored = motor.log().exp()
      // log flips the sign of the motor when s < 0, so compare up to the global sign
      val diff = Math.min((restored - motor).norm, (restored + motor).norm)
      assert(diff <= 1e-14 * motor.norm, s"h = $h, v = $v, motor = $motor, restored = $restored")
    }
  }
