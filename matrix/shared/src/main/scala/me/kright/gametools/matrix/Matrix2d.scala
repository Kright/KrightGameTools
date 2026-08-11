package me.kright.gametools.matrix

import me.kright.arrayview.{ArrayView2d, ArrayView2dFlat}

import me.kright.gametools.mathutil.ExactArith

/** the hardcoded formulas for 2x2 matrices; the arguments may be any views */
object Matrix2d:
  def apply(array: Array[Double]): ArrayView2dFlat[Double] =
    ArrayView2dFlat(array, 2, 2)

  def determinant(a: ArrayView2d[Double]): Double =
    require(a.w == 2 && a.h == 2)
    ExactArith.diffOfProducts(a(0, 0), a(1, 1), a(0, 1), a(1, 0))

  def inverted(a: ArrayView2d[Double]): ArrayView2dFlat[Double] =
    val det = determinant(a) // this may be 0.0, check if necessary
    val d = 1.0 / det

    ArrayView2dFlat(
      Array(
        d * a(1, 1), -d * a(0, 1),
        -d * a(1, 0), d * a(0, 0)
      ), 2, 2)
