package com.github.kright.matrix

import me.kright.arrayview.ArrayView2d
import com.github.kright.math.FastRange

import scala.annotation.targetName

class Matrix(val h: Int, val w: Int) extends ArrayView2d[Double]:
  override val data: Array[Double] = new Array[Double](h * w)

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
    for (y <- FastRange(h);
         x <- FastRange(w)) {
      val v = if (x == y) 1.0 else 0.0
      this (y, x) = v
    }

  @targetName("timesAssign")
  def *=(s: Double): Unit =
    mapElements(_ * s)

  private inline def elementsIndices =
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

    val result = new Matrix(this.h, right.w)

    for (y <- FastRange(result.h);
         x <- FastRange(result.w)) {
      var sum = 0.0
      for (k <- FastRange(this.w)) {
        sum = Math.fma(this (y, k), right(k, x), sum)
      }
      result(y, x) = sum
    }

    result

  def transposedCopy(): Matrix =
    val result = new Matrix(w, h)
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

  def mapElements(f: Double => Double): Unit = {
    for (i <- elementsIndices) {
      data(i) = f(data(i))
    }
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


object Matrix:
  def symmetricMatrixToDiagonalAndEigenvectors(m: Matrix): (Matrix, Matrix) =
    SymmetricMatrixDiagonalization.toDiagonalAndEigenvectors(m)

  def fromValues(h: Int, w: Int)(data: Double*): Matrix = {
    require(data.size == h * w)
    val m = Matrix(h, w)
    data.copyToArray(m.data)
    m
  }

  def idt(size: Int): Matrix =
    val result = new Matrix(size, size)
    for (i <- FastRange(size)) {
      result(i, i) = 1.0
    }
    result