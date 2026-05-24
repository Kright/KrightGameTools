package com.github.kright.matrix

import me.kright.arrayview.ArrayView2d
import com.github.kright.util.{FastRange, IEqualsWithEps}

import scala.annotation.targetName

class Matrix(val h: Int,
             val w: Int,
             override val data: Array[Double]) extends ArrayView2d[Double] with IEqualsWithEps[Matrix]:
  require(data.length == h * w)

  override def shape0: Int = h

  override def shape1: Int = w

  override inline val offset = 0

  override inline def stride0: Int = w

  override inline val stride1 = 1
  override inline val hasSimpleFlatLayout = true

  override def getIndex(y: Int, x: Int): Int =
    y * stride0 + x

  def setIdt(): Unit =
    require(isSquare)
    fill { (x, y) =>
      if (x == y) 1.0 else 0.0
    }

  @targetName("timesAssign")
  def *=(s: Double): Unit = {
    mapInplace(_ * s)
  }

  protected inline def elementsIndices =
    FastRange(data.length)

  @targetName("plusAssign")
  def +=(m: Matrix): Unit =
    require(hasSameSize(m))
    for (i <- elementsIndices) {
      data(i) += m.data(i)
    }

  @targetName("minusAssign")
  def -=(m: Matrix): Unit =
    require(hasSameSize(m))
    for (i <- elementsIndices) {
      data(i) -= m.data(i)
    }

  @targetName("plus")
  def +(m: Matrix): Matrix =
    require(hasSameSize(m))
    val result = Matrix(h, w)
    for (i <- elementsIndices) {
      result.data(i) = data(i) + m.data(i)
    }
    result

  @targetName("minus")
  def -(m: Matrix): Matrix =
    require(hasSameSize(m))
    val result = Matrix(h, w)
    for (i <- elementsIndices) {
      result.data(i) = data(i) - m.data(i)
    }
    result

  @targetName("times")
  def *(right: Matrix): Matrix =
    require(this.w == right.h)

    val result = Matrix(this.h, right.w)

    for (y <- FastRange(result.h);
         x <- FastRange(result.w)) {
      var sum = 0.0
      for (k <- FastRange(this.w)) {
        sum = Math.fma(this (y, k), right(k, x), sum)
      }
      result(y, x) = sum
    }

    result

  @targetName("times")
  def *(scalar: Double): Matrix =
    this.mapTo(_ * scalar, Matrix(h, w))

  @targetName("div")
  def /(scalar: Double): Matrix =
    this * (1 / scalar)

  def transposedCopy(): Matrix =
    val result = Matrix(w, h)
    for (y <- FastRange(result.h);
         x <- FastRange(result.w)) {
      result(y, x) = this (x, y)
    }
    result

  def transposeInplace(): Unit =
    require(isSquare)

    for (i <- FastRange(1, h);
         j <- FastRange(0, i)) {
      val t = this (i, j)
      val t2 = this (j, i)
      this (i, j) = t2
      this (j, i) = t
    }

  def setZero(): Unit =
    for (i <- elementsIndices) {
      data(i) = 0.0
    }

  def copy(): Matrix =
    val r = Matrix(h, w)
    data.copyToArray(r.data)
    r

  def frobeniusNormSquare: Double =
    var sum = 0.0
    for (elem <- data) {
      sum = Math.fma(elem, elem, sum)
    }
    sum

  def frobeniusNorm: Double =
    Math.sqrt(frobeniusNormSquare)

  override def toString: String =
    MatrixPrinter.oneLinePrinter(this)

  def det(): Double = {
    require(h == w)
    if (h == 1) return data(0)
    if (h == 2) return Matrix2d.determinant(this)
    if (h == 3) return Matrix3d.determinant(this)
    if (h == 4) return Matrix4d.determinant(this)
    throw new UnsupportedOperationException("Determinant calculation for matrices larger than 4x4 is not implemented")
  }

  def inverted(): Matrix = {
    require(h == w)
    if (h == 1) return Matrix(1, 1, Array(1.0 / data(0)))
    if (h == 2) return Matrix2d.inverted(this)
    if (h == 3) return Matrix3d.inverted(this)
    if (h == 4) return Matrix4d.inverted(this)
    throw new UnsupportedOperationException("Inversion for matrices larger than 4x4 is not implemented")
  }

  override def isEquals(other: Matrix, eps: Double): Boolean = {
    if (h != other.h || w != other.w) return false
    data.view.zip(other.data).forall((d1, d2) => Math.abs(d1 - d2) <= eps)
  }


object Matrix:
  def apply(h: Int, w: Int): Matrix =
    new Matrix(h, w, new Array[Double](h * w))

  def apply(h: Int, w: Int, data: Array[Double]): Matrix =
    new Matrix(h, w, data)

  def symmetricMatrixToDiagonalAndEigenvectors(m: Matrix): (Matrix, Matrix) =
    SymmetricMatrixDiagonalization.toDiagonalAndEigenvectors(m)

  def fromValues(h: Int, w: Int)(data: Double*): Matrix = {
    require(data.size == h * w)
    Matrix(h, w, data.toArray)
  }

  def idt(size: Int): Matrix =
    val result = Matrix(size, size)
    for (i <- FastRange(size)) {
      result(i, i) = 1.0
    }
    result