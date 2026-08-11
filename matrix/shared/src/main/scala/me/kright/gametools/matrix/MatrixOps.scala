package me.kright.gametools.matrix

import me.kright.arrayview.{ArrayView1d, ArrayView2d, ArrayView2dFlat}
import me.kright.gametools.mathutil.{CanEqualWithEps, ExactArith, FastRange}

import scala.annotation.targetName

/**
 * Linear algebra over 2d views of doubles - there is no matrix class, any [[ArrayView2d]] of
 * doubles is a matrix: a lazily transposed view, a slice or a broadcast of a bigger array
 * works like any other, so e.g. `a * b.transposed` multiplies without materializing the copy.
 * The results are freshly allocated flat views.
 */

def matrixFromValues(h: Int, w: Int)(values: Double*): ArrayView2dFlat[Double] =
  require(values.size == h * w)
  ArrayView2dFlat(values.toArray, h, w)

def diagonalMatrix(values: ArrayView1d[Double]): ArrayView2dFlat[Double] =
  val result = ArrayView2dFlat[Double](values.shape0, values.shape0)
  for (i <- 0 until values.shape0) {
    result(i, i) = values(i)
  }
  result

def identityMatrix(size: Int): ArrayView2dFlat[Double] =
  val result = ArrayView2dFlat[Double](size, size)
  for (i <- 0 until size) {
    result(i, i) = 1.0
  }
  result

extension (m: ArrayView2d[Double])
  /** the height (shape0), matrix-speak */
  inline def h: Int = m.shape0

  /** the width (shape1), matrix-speak */
  inline def w: Int = m.shape1

  def setIdentity(): Unit =
    require(m.isSquare)
    m.fill((y, x) => if (y == x) 1.0 else 0.0)

  @targetName("timesAssign")
  def *=(s: Double): Unit =
    m.mapInplace(_ * s)

  @targetName("plusAssign")
  def +=(other: ArrayView2d[Double]): Unit =
    require(m.hasSameSize(other))
    m.mapWithIndexInplace((v, y, x) => v + other(y, x))

  @targetName("minusAssign")
  def -=(other: ArrayView2d[Double]): Unit =
    require(m.hasSameSize(other))
    m.mapWithIndexInplace((v, y, x) => v - other(y, x))

  @targetName("plus")
  def +(other: ArrayView2d[Double]): ArrayView2dFlat[Double] =
    MatrixAlgebra.plus(m, other)

  @targetName("minus")
  def -(other: ArrayView2d[Double]): ArrayView2dFlat[Double] =
    MatrixAlgebra.minus(m, other)

  @targetName("times")
  def *(right: ArrayView2d[Double]): ArrayView2dFlat[Double] =
    MatrixAlgebra.times(m, right)

  @targetName("times")
  def *(scalar: Double): ArrayView2dFlat[Double] =
    MatrixAlgebra.timesScalar(m, scalar)

  @targetName("div")
  def /(scalar: Double): ArrayView2dFlat[Double] =
    MatrixAlgebra.timesScalar(m, 1 / scalar)

  def transposeInplace(): Unit =
    require(m.isSquare)

    for (i <- FastRange(1, m.h);
         j <- FastRange(0, i)) {
      val t = m(i, j)
      m(i, j) = m(j, i)
      m(j, i) = t
    }

  def frobeniusNormSquare: Double =
    var sum = 0.0
    m.foreach { elem =>
      sum = ExactArith.fma(elem, elem, sum)
    }
    sum

  def frobeniusNorm: Double =
    Math.sqrt(frobeniusNormSquare)

  /** hardcoded formulas for the sizes 1..4, checked at runtime */
  def determinant: Double =
    require(m.isSquare)
    m.h match
      case 1 => m(0, 0)
      case 2 => Matrix2d.determinant(m)
      case 3 => Matrix3d.determinant(m)
      case 4 => Matrix4d.determinant(m)
      case _ => throw new UnsupportedOperationException("determinant is implemented for sizes 1..4")

  /** hardcoded formulas for the sizes 1..4, checked at runtime */
  def inverted: ArrayView2dFlat[Double] =
    require(m.isSquare)
    m.h match
      case 1 => ArrayView2dFlat(Array(1.0 / m(0, 0)), 1, 1)
      case 2 => Matrix2d.inverted(m)
      case 3 => Matrix3d.inverted(m)
      case 4 => Matrix4d.inverted(m)
      case _ => throw new UnsupportedOperationException("inversion is implemented for sizes 1..4")

  def show: String =
    MatrixPrinter.oneLinePrinter(m)


/** the shared implementations behind the extension operators */
private[matrix] object MatrixAlgebra:
  def plus(a: ArrayView2d[Double], b: ArrayView2d[Double]): ArrayView2dFlat[Double] =
    require(a.hasSameSize(b))
    val result = ArrayView2dFlat[Double](a.shape0, a.shape1)
    result.fill((y, x) => a(y, x) + b(y, x))
    result

  def minus(a: ArrayView2d[Double], b: ArrayView2d[Double]): ArrayView2dFlat[Double] =
    require(a.hasSameSize(b))
    val result = ArrayView2dFlat[Double](a.shape0, a.shape1)
    result.fill((y, x) => a(y, x) - b(y, x))
    result

  def times(a: ArrayView2d[Double], b: ArrayView2d[Double]): ArrayView2dFlat[Double] =
    require(a.shape1 == b.shape0)

    val result = ArrayView2dFlat[Double](a.shape0, b.shape1)

    for (y <- FastRange(result.shape0);
         x <- FastRange(result.shape1)) {
      var sum = 0.0
      for (k <- FastRange(a.shape1)) {
        sum = ExactArith.fma(a(y, k), b(k, x), sum)
      }
      result(y, x) = sum
    }

    result

  def timesScalar(a: ArrayView2d[Double], scalar: Double): ArrayView2dFlat[Double] =
    val result = ArrayView2dFlat[Double](a.shape0, a.shape1)
    result.fill((y, x) => a(y, x) * scalar)
    result


given [M <: ArrayView2d[Double]]: CanEqualWithEps[M] with
  extension (a: M)
    def equalsWithEps(b: M, eps: Double): Boolean =
      if (!a.hasSameSize(b)) return false
      for (y <- FastRange(a.shape0);
           x <- FastRange(a.shape1)) {
        if (Math.abs(a(y, x) - b(y, x)) > eps) return false
      }
      true
