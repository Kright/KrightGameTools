package me.kright.gametools.pga2d

import me.kright.gametools.ga.{GARepresentationConfig, MultiVector, PGA2, Signature}
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/**
 * Properties of the generated closed-form dexp/dexpInv, checked against the slow generic
 * series reference in the ga module (PGA2.dexp / PGA2.dexpInv) and against the defining
 * property via finite differences of exp. The 2d sibling of the pga3d DexpTest.
 */
class DexpTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:

  // the same representation the code generator uses, so the blade names match the generated fields
  private given pga2: PGA2 = PGA2(GARepresentationConfig(
    Signature.pga2,
    generatorNames = "wxy",
    namePrefix = "",
    overrideScalar = Option("s"),
    overridePseudoScalar = Option("i"),
  ))

  private def toGa(p: Pga2dProjectivePoint): MultiVector[Double] =
    MultiVector("wx" -> p.wx, "wy" -> p.wy, "xy" -> p.xy)

  private def toPoint(v: MultiVector[Double]): Pga2dProjectivePoint =
    Pga2dProjectivePoint(x = -v("wy"), y = v("wx"), w = v("xy"))

  private def toPoint(v: Pga2dVector): Pga2dProjectivePoint =
    Pga2dProjectivePoint(v.x, v.y, 0.0)

  test("the ga test representation agrees with the generated code (via exp)") {
    forAll(Pga2dGenerators.projectivePoints) { u =>
      assert(toPoint(toGa(u)) == u)

      val gaMotor = PGA2.expForBivector(toGa(u))
      val motor = u.exp
      val diff = Seq(
        gaMotor("s") - motor.s,
        gaMotor("wx") - motor.wx, gaMotor("wy") - motor.wy, gaMotor("xy") - motor.xy,
      ).map(Math.abs).max
      assert(diff < 1e-14, s"u = $u, gaMotor = $gaMotor, motor = $motor")
    }
  }

  private def checkAgainstSeries(closed: Pga2dProjectivePoint, series: MultiVector[Double],
                                 tol: Double, clue: => String): Unit =
    val expected = toPoint(series)
    assert((closed - expected).norm < tol, s"$clue, closed = $closed, expected = $expected")

  test("dexp and dexpInv match the ga series for every argument type") {
    // 1.0: large arguments (bulkNorm <= 1, inside the convergence radius of the dexpInv
    // reference); 1e-4: small, still on the trigonometric branch; 1e-6: the series branch
    for (scale <- Seq(1.0, 1e-4, 1e-6)) {
      forAll(Pga2dGenerators.projectivePoints, Pga2dGenerators.projectivePoints) { (u0, b) =>
        val u = u0 * scale
        val tol = 1e-13 * (1.0 + b.norm) * (1.0 + u.norm)
        checkAgainstSeries(u.dexp(b), PGA2.dexp(toGa(u), toGa(b)), tol, s"u = $u, b = $b")
        checkAgainstSeries(u.dexpInv(b), PGA2.dexpInv(toGa(u), toGa(b)), tol, s"u = $u, b = $b")
        checkAgainstSeries(u.weight.dexp(b), PGA2.dexp(toGa(toPoint(u.weight)), toGa(b)), tol, s"u.weight = ${u.weight}, b = $b")
        checkAgainstSeries(u.weight.dexpInv(b), PGA2.dexpInv(toGa(toPoint(u.weight)), toGa(b)), tol, s"u.weight = ${u.weight}, b = $b")
      }
    }
  }

  test("machine precision for a tiny bulk with a unit-scale weight, across all branch windows") {
    // the hardest inputs for the coefficient formulas: with the narrow (1e-5) series windows
    // the closed forms lost up to ~4e-11 * weightNorm(u) * norm(b) just above the threshold;
    // the wide polynomial windows of SharedFormulas.dexpSinMinusCos/dexpK2 keep the error at
    // a few ulps for every bulk magnitude, which this sweep pins on both sides of the window
    for (bulkScale <- Seq(1.2e-5, 1e-4, 1e-3, 1e-2, 0.1, 0.45, 0.55, 1.0)) {
      forAll(Pga2dGenerators.projectivePoints, Pga2dGenerators.projectivePoints) { (u0, b) =>
        val u = Pga2dProjectivePoint(u0.x, u0.y, if (u0.w < 0) -bulkScale else bulkScale)
        val tol = 1e-14 * (1.0 + b.norm) * (1.0 + u.norm)
        checkAgainstSeries(u.dexp(b), PGA2.dexp(toGa(u), toGa(b)), tol, s"bulkScale = $bulkScale, u = $u, b = $b")
        checkAgainstSeries(u.dexpInv(b), PGA2.dexpInv(toGa(u), toGa(b)), tol, s"bulkScale = $bulkScale, u = $u, b = $b")
      }
    }
  }

  test("dexpInv is the inverse of dexp") {
    forAll(Pga2dGenerators.projectivePoints, Pga2dGenerators.projectivePoints) { (u, b) =>
      val tol = 1e-11 * (1.0 + b.norm)
      assert((u.dexpInv(u.dexp(b)) - b).norm < tol, s"u = $u, b = $b")
      assert((u.weight.dexpInv(u.weight.dexp(b)) - b).norm < tol, s"u.weight = ${u.weight}, b = $b")
    }
  }

  test("the defining property via finite differences: (exp(u + b*h) * exp(u).reverse).log / h") {
    val h = 1e-6
    forAll(Pga2dGenerators.projectivePoints, Pga2dGenerators.projectivePoints) { (u, b) =>
      // the motor exp(u + b*h) * exp(u).reverse is within O(h) of the identity, so its log
      // stays on the principal branch and is well-conditioned for any u
      val finiteDiff = ((u + b * h).exp.geometric(u.exp.reverse)).log / h
      val closed = u.dexp(b)
      val tol = 1e-4 * (1.0 + b.norm) * (1.0 + b.norm) * (1.0 + u.norm)
      assert((finiteDiff - closed).norm < tol, s"u = $u, b = $b, finiteDiff = $finiteDiff, closed = $closed")
    }
  }

  test("collinear arguments pass through exactly") {
    forAll(Pga2dGenerators.projectivePoints) { u =>
      // in 2d each component of cross is a single product difference, so u.cross(u * t) is
      // exactly zero for power-of-two t (the products are bit-identical) and dexp is exact
      for (t <- Seq(1.0, 2.0, 0.5, -4.0)) {
        val b = u * t
        assert(u.dexp(b) == b, s"u = $u, t = $t")
        assert(u.dexpInv(b) == b, s"u = $u, t = $t")
      }
    }
  }

  test("degenerate arguments are exact and NaN-free") {
    forAll(Pga2dGenerators.projectivePoints) { b =>
      assert(Pga2dProjectivePoint.zero.dexp(b) == b)
      assert(Pga2dProjectivePoint.zero.dexpInv(b) == b)
      assert(Pga2dVector.zero.dexp(b) == b)
      assert(Pga2dVector.zero.dexpInv(b) == b)
    }

    forAll(Pga2dGenerators.vectors, Pga2dGenerators.projectivePoints) { (w, b) =>
      // for an ideal u the series terminates: dexp(u, b) = b + u.cross(b), exactly
      assert(w.dexp(b) == b + w.cross(b), s"w = $w, b = $b")
      assert(w.dexpInv(b) == b - w.cross(b), s"w = $w, b = $b")
      // the generic Pga2dProjectivePoint path degenerates to the same exact values
      assert(toPoint(w).dexp(b) == w.dexp(b), s"w = $w, b = $b")
      assert(toPoint(w).dexpInv(b) == w.dexpInv(b), s"w = $w, b = $b")
    }
  }

  test("no jump at the trigonometry/series thresholds") {
    // 1e-5 is the series window of sinDivLen, 0.5 the wide polynomial window of
    // sinMinusCosDivLen2 and k2
    for ((threshold, delta) <- Seq((1e-5, 1e-9), (0.5, 1e-12))) {
      forAll(Pga2dGenerators.projectivePoints.filter(_.bulkNorm > 1e-20), Pga2dGenerators.projectivePoints) { (u0, b) =>
        val direction = u0.normalizedByBulk
        val below = direction * (threshold * (1.0 - delta))
        val above = direction * (threshold * (1.0 + delta))
        val tol = 1e-10 * (1.0 + b.norm) * (1.0 + direction.norm)
        assert((below.dexp(b) - above.dexp(b)).norm < tol, s"threshold = $threshold, u0 = $u0, b = $b")
        assert((below.dexpInv(b) - above.dexpInv(b)).norm < tol, s"threshold = $threshold, u0 = $u0, b = $b")
      }
    }
  }
