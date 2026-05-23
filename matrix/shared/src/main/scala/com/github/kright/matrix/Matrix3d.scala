package com.github.kright.matrix

object Matrix3d:
  def zero: Matrix = Matrix(3, 3)

  def id: Matrix = Matrix3d(Array(
    1.0, 0.0, 0.0,
    0.0, 1.0, 0.0,
    0.0, 0.0, 1.0)
  )

  def apply(array: Array[Double]): Matrix =
    Matrix(3, 3, array)

  def determinant(m: Matrix): Double = {
    require(m.w == 3)
    require(m.h == 3)
    val f = m.data
    Matrix3d.determinant(f(0), f(1), f(2), f(3), f(4), f(5), f(6), f(7), f(8))
  }

  inline def determinant(a00: Double, a01: Double, a02: Double,
                         a10: Double, a11: Double, a12: Double,
                         a20: Double, a21: Double, a22: Double): Double =
    a00 * (a11 * a22 - a21 * a12) +
      a01 * (a12 * a20 - a10 * a22) +
      a02 * (a10 * a21 - a11 * a20)

  def inverted(a: Matrix): Matrix =
    val det = determinant(a) // this may be 0.0, check if necessary
    val d = 1.0 / det
    val arr = a.data

    inline def f(x: Int, y: Int) = arr(y * 3 + x)

    Matrix(3, 3,
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
      )
    )
