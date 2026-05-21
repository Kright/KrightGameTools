package com.github.kright.math


/**
 * m00 m01
 * m10 m11
 */
final class Matrix2d(var m00: Double, var m01: Double,
                     var m10: Double, var m11: Double) extends MatrixNd[Matrix2d]:

  def this() = this(0.0, 0.0, 0.0, 0.0)

  override def copy(): Matrix2d =
    new Matrix2d(m00, m01, m10, m11)

  override def setIdentity(): Matrix2d =
    this := (
      1.0, 0.0,
      0.0, 1.0,
    )


  def *(v: IVector2d): Vector2d =
    Matrix2d.multiply(this, v)


  override def *(right: Matrix2d): Matrix2d =
    Matrix2d.multiply(this, right, new Matrix2d())

  override def *=(right: Matrix2d): Matrix2d =
    Matrix2d.multiply(this, right, result = this)

  override def *>(right: Matrix2d): Matrix2d =
    Matrix2d.multiply(this, right, result = right)

  override def *=(v: Double): Matrix2d =
    Matrix2d.multiply(this, v, result = this)

  override def *(v: Double): Matrix2d =
    Matrix2d.multiply(this, v, new Matrix2d())

  override def +=(m: Matrix2d): Matrix2d =
    Matrix2d.add(this, m, result = this)

  override def +(m: Matrix2d): Matrix2d =
    Matrix2d.add(this, m, result = new Matrix2d())

  override def -=(m: Matrix2d): Matrix2d =
    Matrix2d.sub(this, m, result = this)

  override def -(m: Matrix2d): Matrix2d =
    Matrix2d.sub(this, m, result = new Matrix2d())

  override def madd(m: Matrix2d, v: Double): Matrix2d =
    Matrix2d.multiplyAdd(this, m, v, this)

  override def det(): Double =
    m00 * m11 - m01 * m10

  override def transpose(): Matrix2d =
    val t = m01
    m01 = m10
    m10 = t
    this

  override def transposed(): Matrix2d =
    new Matrix2d(
      m00, m10,
      m01, m11,
    )

  override def invert(): Matrix2d =
    Matrix2d.invertMatrix(this, result = this)

  override def inverted(): Matrix2d =
    Matrix2d.invertMatrix(this, result = new Matrix2d())

  override def :=(m: Matrix2d): Matrix2d =
    m00 = m.m00
    m01 = m.m01
    m10 = m.m10
    m11 = m.m11
    this

  def :=(a00: Double, a01: Double,
         a10: Double, a11: Double): Matrix2d =
    m00 = a00
    m01 = a01
    m10 = a10
    m11 = a11
    this

  def set2dRotation(radians: Double): Matrix2d =
    val sin = Math.sin(radians)
    val cos = Math.cos(radians)
    this := (
      cos, -sin,
      sin, cos,
    )

  def setScale(s: IVector2d): Matrix2d =
    this := (
      s.x, 0.0,
      0.0, s.y
    )

  override def isEquals(other: Matrix2d, eps: Double): Boolean =
    Math.abs(m00 - other.m00) < eps &&
      Math.abs(m01 - other.m01) < eps &&
      Math.abs(m10 - other.m10) < eps &&
      Math.abs(m11 - other.m11) < eps

  override def toString: String =
    s"Matrix2d($m00, $m01, $m10, $m11)"


object Matrix2d extends MatrixNdFactory[Matrix2d]:
  inline def apply(): Matrix2d = new Matrix2d()

  inline def apply(m00: Double, m01: Double, m10: Double, m11: Double) = new Matrix2d(m00, m01, m10, m11)

  override def zero: Matrix2d = new Matrix2d()

  override def id: Matrix2d = new Matrix2d(1.0, 0.0, 0.0, 1.0)

  def multiply(l: Matrix2d, r: Matrix2d, result: Matrix2d): Matrix2d =
    result := (
      l.m00 * r.m00 + l.m01 * r.m10, l.m00 * r.m01 + l.m01 * r.m11,
      l.m10 * r.m00 + l.m11 * r.m10, l.m10 * r.m01 + l.m11 * r.m11,
    )

  def multiply(a: Matrix2d, m: Double, result: Matrix2d): Matrix2d =
    result := (
      a.m00 * m, a.m01 * m,
      a.m10 * m, a.m11 * m
    )

  def multiply(a: Matrix2d, v: IVector2d): Vector2d =
    Vector2d(
      a.m00 * v.x + a.m01 * v.y,
      a.m10 * v.x + a.m11 * v.y,
    )

  private inline def elementWiseOperation(left: Matrix2d, right: Matrix2d, result: Matrix2d)(inline op: (Double, Double) => Double): Matrix2d =
    result := (
      op(left.m00, right.m00), op(left.m01, right.m01),
      op(left.m10, right.m10), op(left.m11, right.m11),
    )

  def add(a: Matrix2d, b: Matrix2d, result: Matrix2d): Matrix2d =
    elementWiseOperation(a, b, result)(_ + _)

  def sub(a: Matrix2d, b: Matrix2d, result: Matrix2d): Matrix2d =
    elementWiseOperation(a, b, result)(_ - _)

  def multiplyAdd(a: Matrix2d, b: Matrix2d, v: Double, result: Matrix2d): Matrix2d =
    elementWiseOperation(a, b, result) { (left, right) => left + right * v }

  def invertMatrix(a: Matrix2d, result: Matrix2d): Matrix2d =
    val det = a.det() // this may be 0.0, check if necessary
    val d = 1.0 / det

    result := (
      d * a.m11, -d * a.m01,
      -d * a.m10, d * a.m00
    )
