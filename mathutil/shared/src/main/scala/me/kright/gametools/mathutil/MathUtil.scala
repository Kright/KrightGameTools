package me.kright.gametools.mathutil

object MathUtil:
  inline val Pi = math.Pi
  inline val Tau = 2.0 * Pi
  inline val TauDiv = 1.0 / Tau

  def isEquals(arr1: Array[Double], arr2: Array[Double], eps: Double): Boolean =
    require(arr1.length == arr2.length, s"arrays have different lengths: ${arr1.length} != ${arr2.length}")
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

  /**
   * like math.min, but if exactly one argument is NaN, returns the other one
   * (math.min propagates NaN). Returns NaN only if both arguments are NaN.
   * Unlike math.min, does not distinguish -0.0 and +0.0.
   */
  inline def minNanSafe(a: Double, b: Double): Double =
    if (b.isNaN) a else if (a < b) a else b

  /**
   * like math.max, but if exactly one argument is NaN, returns the other one
   * (math.max propagates NaN). Returns NaN only if both arguments are NaN.
   * Unlike math.max, does not distinguish -0.0 and +0.0.
   */
  inline def maxNanSafe(a: Double, b: Double): Double =
    if (b.isNaN) a else if (a > b) a else b

  /**
   * minimum of the non-NaN arguments; NaN only if all three are NaN
   */
  inline def minNanSafe(a: Double, b: Double, c: Double): Double =
    minNanSafe(minNanSafe(a, b), c)

  /**
   * maximum of the non-NaN arguments; NaN only if all three are NaN
   */
  inline def maxNanSafe(a: Double, b: Double, c: Double): Double =
    maxNanSafe(maxNanSafe(a, b), c)

  extension (d: Double)
    /**
     * @return value in range [lower, upper] or NaN if d is NaN
     */
    def clamp(lower: Double, upper: Double): Double =
      if (d < lower) lower
      else if (d > upper) upper
      else d

    def square: Double =
      d * d


  extension [T](arr: Array[T])
    inline def swap(i: Int, j: Int): Unit =
      val t = arr(i)
      arr(i) = arr(j)
      arr(j) = t