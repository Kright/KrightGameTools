package me.kright.gametools.mathutil

import me.kright.gametools.mathutil.MathUtil
import me.kright.gametools.mathutil.MathUtil.*
import org.scalatest.funsuite.AnyFunSuite

class MathUtilTest extends AnyFunSuite:
  test("clamp") {
    val lower = -1.1
    val upper = 2.2

    assert(0.0.clamp(lower, upper) == 0.0)
    assert(-3.0.clamp(lower, upper) == lower)
    assert(3.0.clamp(lower, upper) == upper)
    assert(upper.clamp(lower, upper) == upper)
    assert(lower.clamp(lower, upper) == lower)
  }

  test("clamp for NaN") {
    assert(Double.NaN.clamp(-1.0, 1.0).isNaN)
  }

  test("clamp corner cases are inside clamp") {
    val low = -1.0
    val high = 1.0

    for (cornerValues <- Seq(Double.PositiveInfinity, Double.NegativeInfinity, Double.MinValue, Double.MaxValue, Double.MinPositiveValue, 0.0, low, high)) {
      val clamped = cornerValues.clamp(low, high)

      assert(clamped >= low)
      assert(clamped <= high)
      assert(!clamped.isNaN)
    }
  }

  test("sign") {
    assert(0.0.sign == 0.0)
    assert(5.0.sign == 1.0)
    assert(-10.0.sign == -1.0)
  }

  test("pow") {
    assert(MathUtil.pow(2, 10, _ * _) == 1024)
    assert(MathUtil.pow(1, 100, _ + _) == 100)
  }

  test("minNanSafe and maxNanSafe on regular values") {
    assert(minNanSafe(1.0, 2.0) == 1.0)
    assert(minNanSafe(2.0, 1.0) == 1.0)
    assert(maxNanSafe(1.0, 2.0) == 2.0)
    assert(maxNanSafe(2.0, 1.0) == 2.0)

    assert(minNanSafe(Double.NegativeInfinity, 1.0) == Double.NegativeInfinity)
    assert(maxNanSafe(Double.PositiveInfinity, 1.0) == Double.PositiveInfinity)
  }

  test("minNanSafe and maxNanSafe ignore NaN") {
    val nan = Double.NaN

    assert(minNanSafe(nan, 1.0) == 1.0)
    assert(minNanSafe(1.0, nan) == 1.0)
    assert(maxNanSafe(nan, 1.0) == 1.0)
    assert(maxNanSafe(1.0, nan) == 1.0)

    assert(minNanSafe(nan, Double.PositiveInfinity) == Double.PositiveInfinity)
    assert(maxNanSafe(nan, Double.NegativeInfinity) == Double.NegativeInfinity)

    assert(minNanSafe(nan, nan).isNaN)
    assert(maxNanSafe(nan, nan).isNaN)
  }

  test("minNanSafe and maxNanSafe with three arguments") {
    assert(minNanSafe(1.0, 2.0, 3.0) == 1.0)
    assert(minNanSafe(3.0, 2.0, 1.0) == 1.0)
    assert(minNanSafe(2.0, 1.0, 3.0) == 1.0)
    assert(maxNanSafe(1.0, 2.0, 3.0) == 3.0)
    assert(maxNanSafe(3.0, 2.0, 1.0) == 3.0)
    assert(maxNanSafe(2.0, 3.0, 1.0) == 3.0)

    val nan = Double.NaN

    assert(minNanSafe(nan, 2.0, 3.0) == 2.0)
    assert(minNanSafe(2.0, nan, 3.0) == 2.0)
    assert(minNanSafe(2.0, 3.0, nan) == 2.0)
    assert(maxNanSafe(nan, 2.0, 3.0) == 3.0)
    assert(maxNanSafe(2.0, nan, 3.0) == 3.0)
    assert(maxNanSafe(2.0, 3.0, nan) == 3.0)

    assert(minNanSafe(nan, nan, 1.0) == 1.0)
    assert(minNanSafe(nan, 1.0, nan) == 1.0)
    assert(minNanSafe(1.0, nan, nan) == 1.0)
    assert(maxNanSafe(nan, nan, 1.0) == 1.0)
    assert(maxNanSafe(nan, 1.0, nan) == 1.0)
    assert(maxNanSafe(1.0, nan, nan) == 1.0)

    assert(minNanSafe(nan, nan, nan).isNaN)
    assert(maxNanSafe(nan, nan, nan).isNaN)
  }
