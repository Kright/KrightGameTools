package me.kright.gametools.physics1d

import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

class SpringElasticityTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:
  private val softK = 10.0
  private val stiffK = 1000.0
  private val travel = 0.5
  private val elastic = SpringElasticity(softK, travel, stiffK)

  private def slope(e: SpringElasticity, d0: Double, d1: Double): Double =
    (e.force(d1) - e.force(d0)) / (d1 - d0)

  test("force is zero at zero deflection") {
    assert(elastic.force(0.0) == 0.0)
  }

  test("stiffness near zero equals softK") {
    val h = 1e-8 * travel
    assert(Math.abs(slope(elastic, -h, h) + softK) <= 1e-6 * softK)

    // within 1% of the pure softK spring while deflection is small compared to the soft zone
    for (d <- Seq(0.001, 0.005, 0.01).map(_ * travel); sign <- Seq(-1.0, 1.0)) {
      assert(Math.abs(elastic.force(sign * d) + softK * sign * d) <= 0.01 * softK * d)
    }
  }

  test("stiffness far from the soft zone equals stiffK") {
    for ((d0, d1) <- Seq((travel, 2 * travel), (3 * travel, 10 * travel), (-2 * travel, -travel))) {
      assert(Math.abs(slope(elastic, d0, d1) + stiffK) <= 1e-9 * stiffK)
    }
  }

  test("force is an odd function") {
    for (i <- 0 to 3000) {
      val d = (i - 1500) * 0.001 * travel * 2 // covers both u = -2 and u = 2 exactly
      assert(elastic.force(-d) == -elastic.force(d))
    }
  }

  test("force is continuous and C1 across the u = 2 boundary (deflection = softZoneTravel)") {
    // value continuity right at the boundary
    val tiny = 1e-12 * travel
    assert(Math.abs(elastic.force(travel - tiny) - elastic.force(travel)) <= 1e-8)
    assert(Math.abs(elastic.force(travel + tiny) - elastic.force(travel)) <= 1e-8)

    // derivative continuity: one-sided slopes at the boundary both approach -stiffK
    val h = 1e-6 * travel
    val slopeError = (stiffK - softK) * (2 * h / travel) * 4 // f'' is bounded by 2*(stiffK-softK)/travel
    assert(Math.abs(slope(elastic, travel - h, travel) + stiffK) <= slopeError)
    assert(Math.abs(slope(elastic, travel, travel + h) + stiffK) <= slopeError)

    // fine scan across the boundary: secant slopes stay inside [-stiffK, -softK]
    // and change no faster than max|f''| allows => no jump and no kink at u = 2
    val n = 4000
    val d0 = 0.5 * travel
    val step = travel / n
    val slopes = (0 until n).map(i => slope(elastic, d0 + i * step, d0 + (i + 1) * step))
    val maxSecondDerivative = 2 * (stiffK - softK) / travel
    for (i <- slopes.indices) {
      assert(slopes(i) >= -stiffK - 1e-6 && slopes(i) <= -softK + 1e-6)
      if (i > 0) {
        assert(Math.abs(slopes(i) - slopes(i - 1)) <= 2 * maxSecondDerivative * step)
      }
    }
  }

  test("slope never exceeds maxStiffness and force never increases") {
    val kGen = Gen.choose(0.0, 1000.0)
    forAll(kGen, kGen, Gen.choose(1e-3, 10.0), Gen.choose(-5.0, 5.0), Gen.choose(1e-6, 5.0)) {
      (k1, k2, zone, d, dd) =>
        val e = SpringElasticity(k1, zone, k2)
        // finite differences over a small dd carry ~1e-6 of numeric noise
        val s = slope(e, d, d + dd)
        assert(s <= 1e-4)
        assert(s >= -e.maxStiffness * (1 + 1e-9) - 1e-4)
    }
  }

  test("softK == stiffK with a nonzero soft zone is exactly a linear spring") {
    val k = 7.7
    val e = SpringElasticity(k, travel, k)
    for (i <- -20 to 20) {
      val d = i * 0.1 * travel
      assert(e.force(d) == -k * d)
    }
  }
