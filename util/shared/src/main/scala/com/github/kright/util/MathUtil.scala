package com.github.kright.util

object MathUtil:
  inline val Pi = math.Pi
  inline val Tau = 2.0 * Pi
  inline val TauDiv = 1.0 / Tau

  def isEquals(arr1: Array[Double], arr2: Array[Double], eps: Double): Boolean =
    for (i <- FastRange(arr1.length)) {
      if (math.abs(arr1(i) - arr2(i)) > eps) return false
    }
    true

  def pow[T](x: T, power: Int, mult: (T, T) => T): T =
    require(power >= 1)
    if (power == 1) return x

    val xx = mult(x, x)
    if (power % 2 == 0) {
      pow(xx, power / 2, mult)
    }
    else {
      mult(x, pow(xx, power / 2, mult))
    }

  def interpolate(a: Double, b: Double, t: Double): Double =
    a * (1.0 - t) + b * t

  extension (d: Double)
    /**
     * @return value in range [lower, upper] or NaN if d is NaN
     */
    def clamp(lower: Double, upper: Double): Double =
      if (d < lower) lower
      else if (d > upper) upper
      else d

    def sign: Double =
      if (d > 0.0) 1.0
      else if (d < 0.0) -1.0
      else 0.0

    def square: Double =
      d * d


  extension [T](arr: Array[T])
    inline def swap(i: Int, j: Int): Unit =
      val t = arr(i)
      arr(i) = arr(j)
      arr(j) = t