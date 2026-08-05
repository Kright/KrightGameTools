package me.kright.gametools.pga3d

import me.kright.gametools.ga.{GARepresentationConfig, MultiVector, PGA3, Signature}
import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks

/**
 * Properties of the generated closed-form dexp/dexpInv, checked against the slow generic
 * series reference in the ga module (PGA3.dexp / PGA3.dexpInv) and against the defining
 * property via finite differences of exp.
 */
class DexpTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:

  // the same representation the code generator uses, so the blade names match the generated fields
  private given pga3: PGA3 = PGA3(GARepresentationConfig(
    Signature.pga3,
    generatorNames = "wxyz",
    namePrefix = "",
    overrideScalar = Option("s"),
    overridePseudoScalar = Option("i"),
  ))

  private def toGa(b: Pga3dBivector): MultiVector[Double] =
    MultiVector(
      "wx" -> b.wx, "wy" -> b.wy, "wz" -> b.wz,
      "xy" -> b.xy, "xz" -> b.xz, "yz" -> b.yz,
    )

  private def toBivector(v: MultiVector[Double]): Pga3dBivector =
    Pga3dBivector(v("wx"), v("wy"), v("wz"), v("xy"), v("xz"), v("yz"))

  private def toBivector(w: Pga3dBivectorWeight): Pga3dBivector =
    Pga3dBivector(w.wx, w.wy, w.wz, 0.0, 0.0, 0.0)

  private def toBivector(b: Pga3dBivectorBulk): Pga3dBivector =
    Pga3dBivector(0.0, 0.0, 0.0, b.xy, b.xz, b.yz)

  test("the ga test representation agrees with the generated code (via exp)") {
    forAll(Pga3dGenerators.bivectors) { u =>
      assert(toBivector(toGa(u)) == u)

      val gaMotor = PGA3.expForBivector(toGa(u))
      val motor = u.exp
      val diff = Seq(
        gaMotor("s") - motor.s,
        gaMotor("wx") - motor.wx, gaMotor("wy") - motor.wy, gaMotor("wz") - motor.wz,
        gaMotor("xy") - motor.xy, gaMotor("xz") - motor.xz, gaMotor("yz") - motor.yz,
        gaMotor("i") - motor.i,
      ).map(Math.abs).max
      assert(diff < 1e-14, s"u = $u, gaMotor = $gaMotor, motor = $motor")
    }
  }

  private def checkAgainstSeries(closed: Pga3dBivector, series: MultiVector[Double],
                                 tol: Double, clue: => String): Unit =
    val expected = toBivector(series)
    assert((closed - expected).norm < tol, s"$clue, closed = $closed, expected = $expected")

  test("dexp matches the ga series for every argument type") {
    // 1.0: large arguments; 1e-4: small, still on the trigonometric branch; 1e-6: the series branch
    for (scale <- Seq(1.0, 1e-4, 1e-6)) {
      forAll(Pga3dGenerators.bivectors, Pga3dGenerators.bivectors) { (u0, b) =>
        val u = u0 * scale
        val tol = 1e-12 * (1.0 + b.norm) * (1.0 + u.norm)
        checkAgainstSeries(u.dexp(b), PGA3.dexp(toGa(u), toGa(b)), tol, s"u = $u, b = $b")
        checkAgainstSeries(u.bulk.dexp(b), PGA3.dexp(toGa(toBivector(u.bulk)), toGa(b)), tol, s"u.bulk = ${u.bulk}, b = $b")
        checkAgainstSeries(u.weight.dexp(b), PGA3.dexp(toGa(toBivector(u.weight)), toGa(b)), tol, s"u.weight = ${u.weight}, b = $b")
      }
    }
  }

  test("dexp matches the ga series for a small bulk with a large weight") {
    forAll(Pga3dGenerators.bivectors, Pga3dGenerators.bivectors) { (u0, b) =>
      val u = toBivector(u0.weight) + u0.bulk * 1e-4
      val tol = 1e-10 * (1.0 + b.norm) * (1.0 + u.norm)
      checkAgainstSeries(u.dexp(b), PGA3.dexp(toGa(u), toGa(b)), tol, s"u = $u, b = $b")
      checkAgainstSeries(u.dexpInv(b), PGA3.dexpInv(toGa(u), toGa(b)), tol, s"u = $u, b = $b")
    }
  }

  test("dexpInv matches the ga series for every argument type") {
    // 0.5 instead of 1.0: keeps bulkNorm below ~0.9, where the truncated Bernoulli series
    // of the reference still converges to full double precision
    for (scale <- Seq(0.5, 1e-4, 1e-6)) {
      forAll(Pga3dGenerators.bivectors, Pga3dGenerators.bivectors) { (u0, b) =>
        val u = u0 * scale
        val tol = 1e-12 * (1.0 + b.norm) * (1.0 + u.norm)
        checkAgainstSeries(u.dexpInv(b), PGA3.dexpInv(toGa(u), toGa(b)), tol, s"u = $u, b = $b")
        checkAgainstSeries(u.bulk.dexpInv(b), PGA3.dexpInv(toGa(toBivector(u.bulk)), toGa(b)), tol, s"u.bulk = ${u.bulk}, b = $b")
        checkAgainstSeries(u.weight.dexpInv(b), PGA3.dexpInv(toGa(toBivector(u.weight)), toGa(b)), tol, s"u.weight = ${u.weight}, b = $b")
      }
    }
  }

  test("dexpInv is the inverse of dexp") {
    forAll(Pga3dGenerators.bivectors, Pga3dGenerators.bivectors) { (u, b) =>
      val tol = 1e-11 * (1.0 + b.norm)
      assert((u.dexpInv(u.dexp(b)) - b).norm < tol, s"u = $u, b = $b")
      assert((u.bulk.dexpInv(u.bulk.dexp(b)) - b).norm < tol, s"u.bulk = ${u.bulk}, b = $b")
      assert((u.weight.dexpInv(u.weight.dexp(b)) - b).norm < tol, s"u.weight = ${u.weight}, b = $b")
    }
  }

  test("the defining property via finite differences: (exp(u + b*h) * exp(u).reverse).log / h") {
    val h = 1e-6
    forAll(Pga3dGenerators.bivectors, Pga3dGenerators.bivectors) { (u, b) =>
      // the motor exp(u + b*h) * exp(u).reverse is within O(h) of the identity, so its log
      // stays on the principal branch and is well-conditioned for any u
      val finiteDiff = ((u + b * h).exp.geometric(u.exp.reverse)).log / h
      val closed = u.dexp(b)
      val tol = 1e-4 * (1.0 + b.norm) * (1.0 + b.norm) * (1.0 + u.norm)
      assert((finiteDiff - closed).norm < tol, s"u = $u, b = $b, finiteDiff = $finiteDiff, closed = $closed")
    }
  }

  test("collinear arguments pass through exactly") {
    forAll(Pga3dGenerators.bivectors) { u =>
      // the generated cross groups the mirrored product pairs (see SortAntisymmetricPairs),
      // so u.cross(u * t) is exactly zero for power-of-two t and dexp passes b through exactly
      for (t <- Seq(1.0, 2.0, 0.5, -4.0)) {
        val b = u * t
        assert(u.dexp(b) == b, s"u = $u, t = $t")
        assert(u.dexpInv(b) == b, s"u = $u, t = $t")
      }
    }
  }

  test("degenerate arguments are exact and NaN-free") {
    forAll(Pga3dGenerators.bivectors) { b =>
      assert(Pga3dBivector.zero.dexp(b) == b)
      assert(Pga3dBivector.zero.dexpInv(b) == b)
      assert(Pga3dBivectorBulk.zero.dexp(b) == b)
      assert(Pga3dBivectorBulk.zero.dexpInv(b) == b)
      assert(Pga3dBivectorWeight.zero.dexp(b) == b)
      assert(Pga3dBivectorWeight.zero.dexpInv(b) == b)
    }

    forAll(Pga3dGenerators.bivectorWeight, Pga3dGenerators.bivectors) { (w, b) =>
      // for a pure-weight u the series terminates: dexp(u, b) = b + u.cross(b), exactly
      assert(w.dexp(b) == b + w.cross(b), s"w = $w, b = $b")
      assert(w.dexpInv(b) == b - w.cross(b), s"w = $w, b = $b")
      // the generic Pga3dBivector path degenerates to the same exact values
      assert(toBivector(w).dexp(b) == w.dexp(b), s"w = $w, b = $b")
      assert(toBivector(w).dexpInv(b) == w.dexpInv(b), s"w = $w, b = $b")
    }
  }

  test("no jump at the trigonometry/series threshold") {
    forAll(Pga3dGenerators.bivectors.filter(_.bulkNorm > 1e-20), Pga3dGenerators.bivectors) { (u0, b) =>
      val direction = u0.normalizedByBulk
      val below = direction * (1e-5 * (1.0 - 1e-9))
      val above = direction * (1e-5 * (1.0 + 1e-9))
      val tol = 1e-10 * (1.0 + b.norm)
      assert((below.dexp(b) - above.dexp(b)).norm < tol, s"u0 = $u0, b = $b")
      assert((below.dexpInv(b) - above.dexpInv(b)).norm < tol, s"u0 = $u0, b = $b")
    }
  }
