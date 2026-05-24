package me.kright.gametools.matrix

object Matrix2d:
  def zero: Matrix = Matrix(2, 2)
  
  def id: Matrix = Matrix2d(Array(
    1.0, 0.0,
    0.0, 1.0)
  )
  
  def apply(array: Array[Double]): Matrix =
    Matrix(2, 2, array)

  def determinant(a: Matrix): Double =
    require(a.w == 2)
    require(a.h == 2)
    a(0, 0) * a(1, 1) - a(0, 1) * a(1, 0)

  def inverted(a: Matrix): Matrix =
    val det = determinant(a) // this may be 0.0, check if necessary
    val d = 1.0 / det

    Matrix(2, 2,
      Array(
        d * a(1, 1), -d * a(0, 1),
        -d * a(1, 0), d * a(0, 0)
      )
    )






    