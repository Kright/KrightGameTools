package me.kright.gametools.matrix

import me.kright.arrayview.{ArrayView2d, ArrayView2dFlat}


/** the hardcoded formulas for 3x3 matrices; the arguments may be any views */
object Matrix3d:
  def apply(array: Array[Double]): ArrayView2dFlat[Double] =
    ArrayView2dFlat(array, 3, 3)

  def determinant(m: ArrayView2d[Double]): Double = {
    require(m.w == 3 && m.h == 3)
    Matrix3d.determinant(
      m(0, 0), m(0, 1), m(0, 2),
      m(1, 0), m(1, 1), m(1, 2),
      m(2, 0), m(2, 1), m(2, 2))
  }

  inline def determinant(a00: Double, a01: Double, a02: Double,
                         a10: Double, a11: Double, a12: Double,
                         a20: Double, a21: Double, a22: Double): Double =
    a00 * (a11 * a22 - a21 * a12) +
      a01 * (a12 * a20 - a10 * a22) +
      a02 * (a10 * a21 - a11 * a20)

  def inverted(a: ArrayView2d[Double]): ArrayView2dFlat[Double] =
    val det = determinant(a) // this may be 0.0, check if necessary
    val d = 1.0 / det

    inline def f(x: Int, y: Int) = a(y, x)

    ArrayView2dFlat(
      Array(
        d * (f(1, 1) * f(2, 2) - f(2, 1) * f(1, 2)),
        d * (f(2, 0) * f(1, 2) - f(1, 0) * f(2, 2)),
        d * (f(1, 0) * f(2, 1) - f(2, 0) * f(1, 1)),

        d * (f(2, 1) * f(0, 2) - f(0, 1) * f(2, 2)),
        d * (f(0, 0) * f(2, 2) - f(2, 0) * f(0, 2)),
        d * (f(2, 0) * f(0, 1) - f(0, 0) * f(2, 1)),

        d * (f(0, 1) * f(1, 2) - f(1, 1) * f(0, 2)),
        d * (f(1, 0) * f(0, 2) - f(0, 0) * f(1, 2)),
        d * (f(0, 0) * f(1, 1) - f(1, 0) * f(0, 1)),
      ), 3, 3)
