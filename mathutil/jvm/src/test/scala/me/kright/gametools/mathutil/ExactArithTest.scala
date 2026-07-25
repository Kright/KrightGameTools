package me.kright.gametools.mathutil

import org.scalatest.funsuite.AnyFunSuiteLike
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalacheck.Gen

import scala.language.unsafeNulls
import scala.collection.immutable.ArraySeq

class ExactArithTest extends AnyFunSuiteLike with ScalaCheckPropertyChecks:

  private def exact(d: Double) = new java.math.BigDecimal(d)

  private def exactFma(a: Double, b: Double, c: Double) =
    exact(a).multiply(exact(b)).add(exact(c))

  private def exactDiffOfProducts(a: Double, b: Double, c: Double, d: Double) =
    exact(a).multiply(exact(b)).subtract(exact(c).multiply(exact(d)))

  /** fma(a, b, -(a * b)) must recover the product rounding error exactly, on both implementations */
  private def assertExactProductError(a: Double, b: Double): Unit = {
    val p = a * b
    val low = ExactArith.fma(a, b, -p)
    val lowPortable = ExactArith.fmaPortable(a, b, -p)
    assert(low == lowPortable, s"fma vs portable for a = $a, b = $b: $low != $lowPortable")
    assert(exact(a).multiply(exact(b)).compareTo(exact(p).add(exact(low))) == 0,
      s"a * b != p + low for a = $a, b = $b, p = $p, low = $low")
  }

  /** the same Kahan sequence as diffOfProducts, but through the portable fma emulation */
  private def diffOfProductsPortable(a: Double, b: Double, c: Double, d: Double): Double = {
    val cd = c * d
    val err = ExactArith.fmaPortable(c, d, -cd)
    val dop = ExactArith.fmaPortable(a, b, -cd)
    dop - err
  }

  private def assertPreciseDiffOfProducts(a: Double, b: Double, c: Double, d: Double): Unit = {
    val expected = exactDiffOfProducts(a, b, c, d)
    for ((result, tolerance) <- Seq(
      (ExactArith.diffOfProducts(a, b, c, d), 1e-15),
      (diffOfProductsPortable(a, b, c, d), 2e-15))) {
      if (expected.signum == 0) {
        assert(result == 0.0, s"diffOfProducts($a, $b, $c, $d) = $result, expected exact zero")
      } else {
        val err = exact(result).subtract(expected).abs
        assert(err.compareTo(expected.abs.multiply(exact(tolerance))) <= 0,
          s"diffOfProducts($a, $b, $c, $d) = $result, expected $expected")
      }
    }
  }

  private val normalDoubles: Gen[Double] =
    for {
      mantissa <- Gen.choose(-1.0, 1.0).suchThat(m => Math.abs(m) > 0.1)
      exponent <- Gen.choose(-450, 450)
    } yield mantissa * Math.pow(2.0, exponent)

  test("fma recovers the exact rounding error of a product") {
    forAll(normalDoubles, normalDoubles) { (a, b) =>
      whenever(Math.abs(a * b) > 1e-280 && Math.abs(a * b) < 1e280) {
        assertExactProductError(a, b)
      }
    }
  }

  test("fma stays within rounding of the true a * b + c for arbitrary c") {
    forAll(normalDoubles, normalDoubles, normalDoubles) { (a, b, c) =>
      whenever(Math.abs(a * b) > 1e-280 && Math.abs(a * b) < 1e280 && Math.abs(c) < 1e280) {
        val expected = exactFma(a, b, c)
        for ((result, name) <- ArraySeq((ExactArith.fma(a, b, c), "fma"), (ExactArith.fmaPortable(a, b, c), "portable"))) {
          // hardware fma is correctly rounded (<= 0.5 ulp); the portable emulation may
          // double-round, staying within 1 ulp
          val err = exact(result).subtract(expected).abs
          val ulp = exact(Math.ulp(result)).abs
          assert(err.compareTo(ulp) <= 0, s"$name($a, $b, $c) = $result, expected $expected")
        }
      }
    }
  }

  test("fma product-error extraction is exact at extreme magnitudes") {
    val fullMantissa = 9007199254740991.0 // 2^53 - 1, the edge of integer-exact doubles
    val pairs = ArraySeq(
      (1e150, 1e150), (-1e150, 1e130), (1e290, 1e-290), (1e290, 1.0),
      (1e-140, 1e-140), (3.14e200, -2.71e-100), (1e-300, 1e290),
      (Math.PI, Math.E), (1.0 + Math.ulp(1.0), 1.0 - Math.ulp(0.5)),
      (1e15, 1e20), (1e-15, 1e-20), (1e-15, 1e20), (fullMantissa, fullMantissa), (fullMantissa, 3.0),
      (1.0 / fullMantissa, fullMantissa),
      (0.0, 1e300), (-0.0, -1e-140))
    for ((a, b) <- pairs) {
      assertExactProductError(a, b)
      assertExactProductError(b, a)
    }
  }

  test("diffOfProducts keeps relative precision under catastrophic cancellation") {
    forAll(normalDoubles, normalDoubles, Gen.choose(1, 1000)) { (a, b, ulps) =>
      whenever(Math.abs(a * b) > 1e-280 && Math.abs(a * b) < 1e280) {
        var d = b
        for (_ <- 0 until ulps % 4) d = Math.nextUp(d)
        assertPreciseDiffOfProducts(a, b, a, d)
        assertPreciseDiffOfProducts(a, b, b, a)
      }
    }
  }

  test("diffOfProducts at extreme magnitudes and exact cancellations") {
    assert(ExactArith.diffOfProducts(1e150, 1e150, 1e150, 1e150) == 0.0)
    assert(ExactArith.diffOfProducts(1e150, 1e-150, 1e-150, 1e150) == 0.0)
    assert(ExactArith.diffOfProducts(1e-140, 1e-140, -1e-140, 1e-140) == 2e-280)

    assertPreciseDiffOfProducts(1e150, 1e150, 1e150, Math.nextUp(1e150))
    assertPreciseDiffOfProducts(1e-140, 1e-140, 1e-140, Math.nextUp(1e-140))
    assertPreciseDiffOfProducts(1e290, 1e-290, Math.nextUp(1e-290), 1e290)
    assertPreciseDiffOfProducts(12345.6789, 98765.4321, 98765.4321, 12345.6789)
  }

  test("diffOfProducts recovers the wedge of nearly antipodal unit vectors") {
    val (fx, fy, fz) = (3.0 / 13, 4.0 / 13, 12.0 / 13)
    val perpNorm = Math.sqrt(fx * fx + fy * fy)
    val (px, py, pz) = (fy / perpNorm, -fx / perpNorm, 0.0)

    for (eps <- ArraySeq(1e-3, 1e-8, 1e-10, 1e-13, 1e-16, 0.0)) {
      val (tx, ty, tz) = (
        -(fx * Math.cos(eps) + px * Math.sin(eps)),
        -(fy * Math.cos(eps) + py * Math.sin(eps)),
        -(fz * Math.cos(eps) + pz * Math.sin(eps)))
      assertPreciseDiffOfProducts(fy, tx, fx, ty)
      assertPreciseDiffOfProducts(fz, tx, fx, tz)
      assertPreciseDiffOfProducts(fz, ty, fy, tz)
    }
  }
