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
