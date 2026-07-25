package me.kright.gametools.pga2d

import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import scala.collection.immutable.ArraySeq

/**
 * Corner-case precision tests for the non-trivial numerical methods of pga2d:
 * exp/log, rotation construction, restore, motor renormalization and split,
 * normalization and distanceTo.
 *
 * Assertions are RELATIVE wherever the quantity has a scale, so tiny inputs
 * (1e-10 .. 1e-300) must survive round trips to near machine precision instead of
 * passing trivially under an absolute epsilon.
 */
class PrecisionTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:

  /** relative difference; 0.0 for an exact match (including 0.0 vs -0.0) */
  private def relDiff(actual: Double, expected: Double): Double =
    if (actual == expected) 0.0
    else Math.abs(actual - expected) / Math.max(Math.abs(actual), Math.abs(expected))

  private def assertRel(actual: Double, expected: Double, eps: Double, clue: => String): Unit = {
    val diff = relDiff(actual, expected)
    assert(diff <= eps, s"$clue: actual = $actual, expected = $expected, relDiff = $diff")
  }

  test("projective point exp/log round trip is relatively precise for any half-angle") {
    // half-angles: zero, denormal-adjacent, tiny, both sides of the 1e-5 series/sin branch
    // threshold of exp() and log(), moderate, and close to pi/2 (where motor.s crosses 0)
    val halfAngles = ArraySeq(
      0.0, 1e-300, 1e-100, 1e-50, 1e-20, 1e-10, 1e-6,
      9.9e-6, 1.01e-5, 2e-5, 1e-4, 1e-3, 1e-2, 0.1, 1.0, 1.5, Math.PI / 2 - 1e-6)
    // exp/log do not mix x and y, so even wildly mismatched translation components
    // must be preserved with per-component relative precision
    val shifts = ArraySeq(
      (0.0, 0.0), (1.0, -2.0), (1e-10, 2e-10), (1e-15, -2e-15), (1e-20, -1e-20), (-1e-100, 1e-100),
      (1e10, -1e-10), (1e15, -2e15), (1e20, -1e20))

    for (w <- halfAngles; sign <- ArraySeq(1.0, -1.0); (sx, sy) <- shifts) {
      val p = Pga2dProjectivePoint(sx, sy, sign * w)
      val restored = p.exp().log()
      assertRel(restored.x, p.x, 1e-14, s"x of $p")
      assertRel(restored.y, p.y, 1e-14, s"y of $p")
      assertRel(restored.w, p.w, 1e-14, s"w of $p")
    }
  }

  test("exp sin(len)/len is continuous and accurate across the 1e-5 branch threshold") {
    for (w <- ArraySeq(1e-300, 1e-20, 1e-7, 1e-6, 5e-6, 9.999999e-6, 1.0000001e-5, 2e-5, 1e-4, 1e-3)) {
      val motor = Pga2dProjectivePoint(0.0, 0.0, w).exp()
      assertRel(motor.xy, Math.sin(w), 5e-16, s"sin of half-angle $w")
      assertRel(motor.s, Math.cos(w), 5e-16, s"cos of half-angle $w")
    }
  }

  test("exp(t) matches (p * t).exp() at extreme t") {
    val points = ArraySeq(
      Pga2dProjectivePoint(0.3, -0.4, 0.5),
      Pga2dProjectivePoint(1e-100, 1e-100, 1e-100),
      Pga2dProjectivePoint(0.0, 0.0, 1.0))
    for (t <- ArraySeq(0.0, 1e-300, 1e-100, 1e-20, 1e-15, 1e-10, 1.0, 1e10, 1e15, 1e20); p <- points) {
      val viaT = p.exp(t)
      val viaScale = (p * t).exp()
      assertRel(viaT.s, viaScale.s, 1e-14, s"s for p = $p, t = $t")
      assertRel(viaT.wx, viaScale.wx, 1e-14, s"wx for p = $p, t = $t")
      assertRel(viaT.wy, viaScale.wy, 1e-14, s"wy for p = $p, t = $t")
      assertRel(viaT.xy, viaScale.xy, 1e-14, s"xy for p = $p, t = $t")
    }
  }

  test("vector exp and translator log round trips are exact at any magnitude") {
    for (m <- ArraySeq(0.0, 1e-300, 1e-100, 1e-20, 1e-15, 1e-10, 1.0, 1e10, 1e15, 1e20, 1e100, 1e300);
         (x, y) <- ArraySeq((m, 0.0), (0.0, -m), (m, -m), (m, 1.0))) {
      val v = Pga2dVector(x, y)
      assert((v.exp().log() - v).norm == 0.0, s"v = $v")
      val tr = Pga2dTranslator(x, y)
      assert((tr.log().exp() - tr).norm == 0.0, s"tr = $tr")
    }
  }

  test("motor log is exact for pure translation motors") {
    for (m <- ArraySeq(0.0, 1e-300, 1e-100, 1e-20, 1e-15, 1e-10, 1.0, 1e10, 1e15, 1e20, 1e100)) {
      val motor = Pga2dMotor(s = 1.0, wx = m, wy = -m, xy = 0.0)
      val expected = Pga2dProjectivePoint(x = m, y = m, w = 0.0)
      assert((motor.log() - expected).norm == 0.0, s"motor = $motor")
    }
  }

  test("motor log/exp round trip at corner half-angles and translations") {
    for (h <- ArraySeq(0.0, 1e-10, 1e-4, 0.5, Math.PI / 2 - 1e-8, Math.PI / 2 + 0.3, 3.0);
         v <- ArraySeq(Pga2dVector(0, 0), Pga2dVector(1e-20, 1e-20), Pga2dVector(3, -4))) {
      val rotor = Pga2dRotor(Math.cos(h), Math.sin(h))
      val motor = Pga2dTranslator.addVector(v).geometric(rotor)
      val restored = motor.log().exp()
      // log flips the sign of the motor when s < 0, so compare up to the global sign
      val diff = Math.min((restored - motor).norm, (restored + motor).norm)
      assert(diff <= 1e-14 * motor.norm, s"h = $h, v = $v, motor = $motor, restored = $restored")
    }
  }

  test("rotation preserves tiny angles between vectors instead of stalling") {
    for (angle <- ArraySeq(1e-8, 1e-10, 1e-20, 1e-50, 1e-100, 1e-200, 1e-300)) {
      val from = Pga2dVector(1, 0)
      val to = Pga2dVector(Math.cos(angle), Math.sin(angle))
      val r = Pga2dRotor.rotation(from, to)
      assert(r.xy != 0.0, s"rotation by $angle collapsed to identity")
      val rotated = r.sandwich(from)
      assertRel(rotated.x, to.x, 1e-14, s"x for angle $angle")
      assertRel(rotated.y, to.y, 1e-14, s"y for angle $angle")
    }
  }

  test("rotation works at extreme vector magnitudes") {
    // sqrt(from.normSquare * to.normSquare) used to overflow (garbage rotor at ~1e100)
    // or underflow (NaN at ~1e-100); norms must only be multiplied after their sqrt
    val expected = Math.sqrt(0.5)
    for (scale <- ArraySeq(1e-140, 1e-100, 1e-50, 1e-20, 1e-15, 1e-10, 1.0, 1e10, 1e15, 1e20, 1e50, 1e100, 1e150)) {
      val r = Pga2dRotor.rotation(Pga2dVector(scale, 0.0), Pga2dVector(0.0, scale))
      assertRel(r.s, expected, 1e-15, s"s at scale $scale")
      assertRel(Math.abs(r.xy), expected, 1e-15, s"|xy| at scale $scale")
      val rotated = r.sandwich(Pga2dVector(scale, 0.0))
      assert((rotated - Pga2dVector(0.0, scale)).norm <= 1e-15 * scale, s"rotated at scale $scale")
    }
  }

  test("rotation for nearly antipodal vectors keeps the deviation from pi") {
    // the wedge is recomputed with ExactArith.diffOfProducts and the deviation goes through
    // the well-conditioned asin, so it survives at relative precision for any input;
    // all these angles are within acos(0.9) of pi, i.e. inside the exact-wedge branch
    val from = Pga2dVector(1, 0)
    for (angleToPi <- ArraySeq(0.0, 1e-300, 1e-12, 1e-9, 3e-8, 1e-7, 1e-6, 1e-4, 1e-3, 0.1, 0.4)) {
      val to = Pga2dVector(-Math.cos(angleToPi), Math.sin(angleToPi))
      val r = Pga2dRotor.rotation(from, to)
      assert(Math.abs(r.norm - 1.0) < 1e-14, s"norm at pi - $angleToPi")
      val rotated = r.sandwich(from)
      assert((rotated - to).norm <= 1e-13, s"rotated at pi - $angleToPi: $rotated vs $to")
      if (angleToPi > 0.0) {
        assert(rotated.y != 0.0, s"deviation $angleToPi from pi was dropped")
        assertRel(rotated.y, to.y, 1e-14, s"deviation at pi - $angleToPi")
      }
    }
  }

  test("rotation near antipodal preserves the deviation for generic directions") {
    // components of generic from/to have no zeros, so the naive wedge would cancel
    // catastrophically; this is the case the exact products are for
    val from = Pga2dVector(0.6, 0.8)
    val perp = Pga2dVector(0.8, -0.6)
    for (eps <- ArraySeq(0.4, 0.1, 1e-2, 1.4e-3, 1e-4, 1e-6, 1e-8, 1e-10, 1e-12, 1e-14, 1e-16)) {
      val to = -(from * Math.cos(eps) + perp * Math.sin(eps))
      val r = Pga2dRotor.rotation(from, to)
      val err = (r.sandwich(from) - to).norm
      assert(err <= 3e-15, s"err = $err at pi - $eps")
    }
  }

  test("rotation is accurate on both sides of the main-branch boundary") {
    // the exact-wedge branch starts at dot <= -0.9, i.e. eps0 = acos(0.9) ~ 0.451 away
    // from pi; the guard keeps (1 + dot) >= 0.1 in the main branch and the asin argument
    // below sin(eps0) ~ 0.44 in the exact-wedge branch, so both sides stay at ~1e-15
    val eps0 = Math.acos(0.9)
    val from = Pga2dVector(0.6, 0.8)
    val perp = Pga2dVector(0.8, -0.6)
    for (eps <- ArraySeq(eps0 * 0.98, eps0 * 0.999, eps0, eps0 * 1.001, eps0 * 1.02)) {
      val to = -(from * Math.cos(eps) + perp * Math.sin(eps))
      val r = Pga2dRotor.rotation(from, to)
      assert(Math.abs(r.norm - 1.0) <= 1e-14, s"norm at pi - $eps")
      val err = (r.sandwich(from) - to).norm
      assert(err <= 5e-15, s"err = $err at pi - $eps")
    }
  }

  test("rotation half-angle rotor is relative-exact throughout the antipodal zone") {
    // to = (-cos(eps), sin(eps)) is at the angle (pi - eps) from from = (1, 0);
    // expected rotor: s = sin(eps / 2), |xy| = cos(eps / 2)
    val from = Pga2dVector(1, 0)
    for (eps <- ArraySeq(1e-10, 0.5e-8, 0.99e-8, 1.01e-8, 2e-8, 1e-6, 1e-4, 1e-3, 1e-2, 0.4)) {
      val r = Pga2dRotor.rotation(from, Pga2dVector(-Math.cos(eps), Math.sin(eps)))
      assertRel(r.s, Math.sin(eps / 2), 5e-16, s"s at eps = $eps")
      assertRel(Math.abs(r.xy), Math.cos(eps / 2), 1e-15, s"|xy| at eps = $eps")
    }
  }

  test("rotation for exact-boundary inputs") {
    // from == to: exact identity rotor
    val same = Pga2dRotor.rotation(Pga2dVector(1, 0), Pga2dVector(1, 0))
    assert(same.s == 1.0 && same.xy == 0.0, s"identity: $same")

    // exactly antipodal (sin2a == 0.0, dot == -1.0): the exact pi rotor, applied exactly
    val pi = Pga2dRotor.rotation(Pga2dVector(1, 0), Pga2dVector(-1, 0))
    assert(pi.s == 0.0 && Math.abs(pi.xy) == 1.0, s"pi rotor: $pi")
    assert((pi.sandwich(Pga2dVector(1, 0)) - Pga2dVector(-1, 0)).norm == 0.0)

    // exactly antipodal at a tiny magnitude: still the exact pi rotor
    val from = Pga2dVector(3e-100, 4e-100)
    val piTiny = Pga2dRotor.rotation(from, -from)
    assert(piTiny.s == 0.0 && Math.abs(piTiny.xy) == 1.0, s"tiny pi rotor: $piTiny")
    assert((piTiny.sandwich(from) - (-from)).norm == 0.0)

    // the lineCentral overload hits the same code path directly
    val piLine = Pga2dRotor.rotation(Pga2dLineCentral(1, 0), Pga2dLineCentral(-1, 0))
    assert(piLine.s == 0.0 && Math.abs(piLine.xy) == 1.0, s"lineCentral pi rotor: $piLine")
    val tinyLine = Pga2dRotor.rotation(Pga2dLineCentral(1, 0), Pga2dLineCentral(1, 1e-200))
    assert(tinyLine.s == 1.0, s"lineCentral tiny rotation: $tinyLine")
    assertRel(Math.abs(tinyLine.xy), 5e-201, 1e-15, s"lineCentral tiny rotation: $tinyLine")
  }

  test("motor log angle recovery is continuous across the 1e-5 branch threshold") {
    // motor for the half-angle w; log must return w through atan2 on both sides of the
    // b = angle/sin(angle) series/sqrt branch split
    for (w <- ArraySeq(9.99e-6, 9.999999e-6, 1.0000001e-5, 1.001e-5)) {
      val motor = Pga2dMotor(s = Math.cos(w), wx = 0.0, wy = 0.0, xy = Math.sin(w))
      assertRel(motor.log().w, w, 1e-15, s"recovered half-angle for w = $w")
    }
  }

  test("restore preserves tiny rotations to relative precision") {
    for (theta <- ArraySeq(0.0, 1e-300, 1e-100, 1e-20, 1e-10, 1e-5, 1e-3, 0.1)) {
      val r0 = Pga2dRotor(Math.cos(theta / 2), Math.sin(theta / 2))
      val restored = Pga2dRotor.restore(r0.sandwich(Pga2dVector(1, 0)), r0.sandwich(Pga2dVector(0, 1)))
      val aligned = if (restored.s * r0.s + restored.xy * r0.xy < 0) -restored else restored
      assertRel(aligned.s, r0.s, 1e-14, s"s for theta = $theta")
      assertRel(aligned.xy, r0.xy, 1e-14, s"xy for theta = $theta")
    }
  }

  test("restore preserves rotations near pi to relative precision") {
    // rotor for the angle (pi - delta): s = sin(delta / 2), xy = cos(delta / 2),
    // constructed directly so that tiny delta is not lost in (pi - delta) rounding
    for (delta <- ArraySeq(0.0, 1e-300, 1e-100, 1e-16, 1e-10, 1e-5, 0.05)) {
      val r0 = Pga2dRotor(Math.sin(delta / 2), Math.cos(delta / 2))
      val restored = Pga2dRotor.restore(r0.sandwich(Pga2dVector(1, 0)), r0.sandwich(Pga2dVector(0, 1)))
      val aligned = if (restored.s * r0.s + restored.xy * r0.xy < 0) -restored else restored
      assertRel(aligned.s, r0.s, 1e-13, s"s for delta = $delta")
      assertRel(aligned.xy, r0.xy, 1e-13, s"xy for delta = $delta")
    }
  }

  test("motor renormalization at extreme scales") {
    val motor = Pga2dTranslator.addVector(Pga2dVector(3, -4)).geometric(Pga2dRotor(Math.cos(0.6), Math.sin(0.6)))
    val expected = motor.renormalized
    // bulk components stay in the representable range of squares for scales up to ~1e150
    for (k <- ArraySeq(1e-100, 1e-20, 1e-15, 1e-3, 1.0, 1e3, 1e15, 1e20, 1e100, 1e150)) {
      val renormalized = (motor * k).renormalized
      assertRel(renormalized.s, expected.s, 1e-14, s"s for k = $k")
      assertRel(renormalized.wx, expected.wx, 1e-14, s"wx for k = $k")
      assertRel(renormalized.wy, expected.wy, 1e-14, s"wy for k = $k")
      assertRel(renormalized.xy, expected.xy, 1e-14, s"xy for k = $k")

      val mm = renormalized.geometric(renormalized.reverse)
      assert((mm - Pga2dMotor(s = 1.0)).norm < 1e-14, s"m * ~m for k = $k")
    }
  }

  test("motor split preserves tiny translations and recovers the rotor exactly") {
    for (h <- ArraySeq(0.0, 1e-10, 0.6, 2.5);
         v <- ArraySeq(Pga2dVector(0, 0), Pga2dVector(1e-100, -1e-100), Pga2dVector(1e-20, 1e-20), Pga2dVector(1e-15, -1e-20), Pga2dVector(3, -4), Pga2dVector(1e15, -1e20))) {
      val rotor = Pga2dRotor(Math.cos(h), Math.sin(h))
      val tr = Pga2dTranslator.addVector(v)

      // motor = T * R: toTranslatorAndRotor must give T and R back
      val (t1, r1) = tr.geometric(rotor).toTranslatorAndRotor
      assert(r1 == rotor, s"rotor from T*R split, h = $h, v = $v")
      val trNorm = Pga2dVector(tr.wx, tr.wy).norm
      assert(Pga2dVector(t1.wx - tr.wx, t1.wy - tr.wy).norm <= 1e-14 * trNorm, s"translator from T*R split, h = $h, v = $v, t1 = $t1, tr = $tr")

      // motor = R * T: toRotorAndTranslator must give R and T back
      val (r2, t2) = rotor.geometric(tr).toRotorAndTranslator
      assert(r2 == rotor, s"rotor from R*T split, h = $h, v = $v")
      assert(Pga2dVector(t2.wx - tr.wx, t2.wy - tr.wy).norm <= 1e-14 * trNorm, s"translator from R*T split, h = $h, v = $v, t2 = $t2, tr = $tr")
    }
  }

  test("distance between points is exact for power-of-two separations across magnitudes") {
    // 3-4-5 triples scaled by powers of two are exactly representable, so distanceTo
    // has one correctly rounded answer: exactly 5 * delta.
    // 2^-500 keeps (3 * delta)^2 in the normal range; the squares under the sqrt limit
    // the method to separations in ~[1e-150, 1e150]
    for (e <- ArraySeq(-500, -333, -100, -20, 0, 100, 500)) {
      val delta = math.pow(2.0, e)
      val p1 = Pga2dPoint(0.0, 0.0)
      val p2 = Pga2dPoint(3 * delta, 4 * delta)
      assert(p1.distanceTo(p2) == 5 * delta, s"delta = 2^$e")
      assert(p2.distanceTo(p2) == 0.0, s"delta = 2^$e")
    }
  }

  test("distance between points is relatively precise for tiny separations") {
    for (delta <- ArraySeq(1e-10, 1e-20, 1e-50, 1e-100, 1e-150)) {
      val d = Pga2dPoint(0.0, 0.0).distanceTo(Pga2dPoint(3 * delta, 4 * delta))
      assertRel(d, 5 * delta, 1e-15, s"delta = $delta")
    }
  }

  test("norm and normalizedByNorm at extreme magnitudes") {
    for (scale <- ArraySeq(1e-100, 1e-50, 1e-20, 1e-15, 1e-10, 1.0, 1e10, 1e15, 1e20, 1e50, 1e150)) {
      val v = Pga2dVector(3 * scale, -4 * scale)
      assertRel(v.norm, 5 * scale, 1e-15, s"vector norm at $scale")
      val n = v.normalizedByNorm
      assertRel(n.x, 0.6, 1e-15, s"normalized x at $scale")
      assertRel(n.y, -0.8, 1e-15, s"normalized y at $scale")

      val p = Pga2dProjectivePoint(3 * scale, 4 * scale, 12 * scale)
      assertRel(p.bulkNorm, 12 * scale, 1e-15, s"point bulkNorm at $scale")
      assertRel(p.weightNorm, 5 * scale, 1e-15, s"point weightNorm at $scale")
      assertRel(p.norm, 13 * scale, 1e-15, s"point norm at $scale")
      val byWeight = p.normalizedByWeight
      assertRel(byWeight.x, 0.6, 1e-15, s"normalizedByWeight x at $scale")
      assertRel(byWeight.y, 0.8, 1e-15, s"normalizedByWeight y at $scale")
      assertRel(byWeight.w, 2.4, 1e-15, s"normalizedByWeight w at $scale")

      val motor = Pga2dMotor(s = 3 * scale, wx = 3 * scale, wy = 4 * scale, xy = 4 * scale)
      assertRel(motor.bulkNorm, 5 * scale, 1e-15, s"motor bulkNorm at $scale")
      assertRel(motor.weightNorm, 5 * scale, 1e-15, s"motor weightNorm at $scale")
    }
  }
