package com.github.kright.math

import com.github.kright.math.MathUtil.swap


/**
 * indices in array:
 * 0 1 2
 * 3 4 5
 * 6 7 8
 */
final class Matrix3d(val elements: Array[Double]) extends MatrixNd[Matrix3d]:
  def this() = this(new Array[Double](9))

  inline def apply(y: Int, x: Int): Double = elements(x + y * 3)

  inline def update(y: Int, x: Int, value: Double): Unit = elements(x + y * 3) = value

  override def copy(): Matrix3d =
    new Matrix3d(elements.clone())

  override def setIdentity(): Matrix3d =
    this := (
      1.0, 0.0, 0.0,
      0.0, 1.0, 0.0,
      0.0, 0.0, 1.0
    )


  def *(v: IVector3d): Vector3d =
    Matrix3d.multiply(this, v)

  def *(v: IVector2d): Vector2d =
    Matrix3d.multiply(this, v, 1.0)

  def rotate(v: IVector2d): Vector2d =
    Matrix3d.rotate2d(this, v)


  override def *(right: Matrix3d): Matrix3d =
    Matrix3d.multiply(this, right, new Matrix3d())

  override def *=(right: Matrix3d): Matrix3d =
    Matrix3d.multiply(this, right, result = this)

  override def *>(right: Matrix3d): Matrix3d =
    Matrix3d.multiply(this, right, result = right)

  override def *=(v: Double): Matrix3d =
    Matrix3d.multiply(this, v, result = this)

  override def *(v: Double): Matrix3d =
    Matrix3d.multiply(this, v, new Matrix3d())

  override def +=(m: Matrix3d): Matrix3d =
    Matrix3d.add(this, m, result = this)

  override def +(m: Matrix3d): Matrix3d =
    Matrix3d.add(this, m, result = new Matrix3d())

  override def -=(m: Matrix3d): Matrix3d =
    Matrix3d.sub(this, m, result = this)

  override def -(m: Matrix3d): Matrix3d =
    Matrix3d.sub(this, m, result = new Matrix3d())

  override def madd(m: Matrix3d, v: Double): Matrix3d =
    Matrix3d.multiplyAdd(this, m, v, this)

  override def det(): Double =
    val f = elements
    Matrix3d.determinant(f(0), f(1), f(2), f(3), f(4), f(5), f(6), f(7), f(8))

  override def transpose(): Matrix3d =
    elements.swap(1, 3)
    elements.swap(2, 6)
    elements.swap(5, 7)
    this

  override def invert(): Matrix3d =
    Matrix3d.invertMatrix(this, result = this)

  override def inverted(): Matrix3d =
    Matrix3d.invertMatrix(this, result = new Matrix3d())

  def setRow(row: Int, v: IVector3d): Matrix3d =
    val pos = row * 3
    elements(pos) = v.x
    elements(pos + 1) = v.y
    elements(pos + 2) = v.z
    this

  def setRows(r0: IVector3d, r1: IVector3d, r2: IVector3d): Matrix3d =
    setRow(0, r0)
    setRow(1, r1)
    setRow(2, r2)

  def setColumn(column: Int, v: IVector3d): Matrix3d =
    elements(column) = v.x
    elements(column + 3) = v.y
    elements(column + 6) = v.z
    this

  def setColumns(c0: IVector3d, c1: IVector3d, c2: IVector3d): Matrix3d =
    setColumn(0, c0)
    setColumn(1, c1)
    setColumn(2, c2)

  override def :=(m: Matrix3d): Matrix3d =
    System.arraycopy(m.elements, 0, elements, 0, 9)
    this

  def :=(m: Matrix2d): Matrix3d =
    this := (
      m.m00, m.m01, 0.0,
      m.m10, m.m11, 0.0,
      0.0, 0.0, 1.0
    )

  def setRotation(m: Matrix4d): Matrix3d =
    this := (
      m(0, 0), m(0, 1), m(0, 2),
      m(1, 0), m(1, 1), m(1, 2),
      m(2, 0), m(2, 1), m(2, 2),
    )

  def setRotation(forwardZ: IVector3d, upY: IVector3d): Matrix3d =
    val z = forwardZ.normalized()
    val x = upY.cross(z).normalize()
    val y = z.cross(x)
    this := (
      x.x, x.y, x.z,
      y.x, y.y, y.z,
      z.x, z.y, z.z,
    )

  def :=(q: Quaternion): Matrix3d =
    this := (
      q.rotM00, q.rotM01, q.rotM02,
      q.rotM10, q.rotM11, q.rotM12,
      q.rotM20, q.rotM21, q.rotM22,
    )

  def :=(euler: EulerAngles): Matrix3d =
    val cy = Math.cos(euler.yaw)
    val sy = Math.sin(euler.yaw)
    val cp = Math.cos(euler.pitch)
    val sp = Math.sin(euler.pitch)
    val cr = Math.cos(euler.roll)
    val sr = Math.sin(euler.roll)

    this := (
      cy * cr + sy * sp * sr, -cy * sr + sy * sp * cr, sy * cp,
      cp * sr, cp * cr, -sp,
      -sy * cr + cy * sp * sr, sy * sr + cy * sp * cr, cy * cp,
    )

  def :=(a00: Double, a01: Double, a02: Double,
         a10: Double, a11: Double, a12: Double,
         a20: Double, a21: Double, a22: Double): Matrix3d =
    val e = elements
    e(0) = a00;
    e(1) = a01;
    e(2) = a02
    e(3) = a10;
    e(4) = a11;
    e(5) = a12
    e(6) = a20;
    e(7) = a21;
    e(8) = a22
    this

  def set2dRotation(radians: Double): Matrix3d =
    val sin = Math.sin(radians)
    val cos = Math.cos(radians)
    this := (
      cos, -sin, 0.0,
      sin, cos, 0.0,
      0.0, 0.0, 1.0,
    )

  def set2dTranslation(shift: IVector2d): Matrix3d =
    this := (
      1.0, 0.0, shift.x,
      0.0, 1.0, shift.y,
      0.0, 0.0, 1.0
    )

  def setScale(s: IVector2d): Matrix3d =
    this := (
      s.x, 0.0, 0.0,
      0.0, s.y, 0.0,
      0.0, 0.0, 1.0,
    )

  def setScale(s: IVector3d): Matrix3d =
    this := (
      s.x, 0.0, 0.0,
      0.0, s.y, 0.0,
      0.0, 0.0, s.z,
    )

  override def isEquals(other: Matrix3d, eps: Double): Boolean =
    MathUtil.isEquals(elements, other.elements, eps)

  override def toString: String =
    s"Matrix3d(${elements.mkString(", ")})"


object Matrix3d extends MatrixNdFactory[Matrix3d]:
  inline def apply(): Matrix3d = new Matrix3d()

  override def zero: Matrix3d = new Matrix3d()

  override def id: Matrix3d = new Matrix3d().setIdentity()

  def multiply(left: Matrix3d, right: Matrix3d, result: Matrix3d): Matrix3d =
    inline def f(y: Int, x: Int) = left(y, 0) * right(0, x) + left(y, 1) * right(1, x) + left(y, 2) * right(2, x)

    result := (
      f(0, 0), f(0, 1), f(0, 2),
      f(1, 0), f(1, 1), f(1, 2),
      f(2, 0), f(2, 1), f(2, 2),
    )

  def multiply(a: Matrix3d, m: Double, result: Matrix3d): Matrix3d =
    for (i <- FastRange(9)) {
      result.elements(i) = a.elements(i) * m
    }
    result

  def multiply(a: Matrix3d, v: IVector3d): Vector3d =
    val e = a.elements
    Vector3d(
      e(0) * v.x + e(1) * v.y + e(2) * v.z,
      e(3) * v.x + e(4) * v.y + e(5) * v.z,
      e(6) * v.x + e(7) * v.y + e(8) * v.z,
    )

  def multiply(a: Matrix3d, v: IVector2d, z: Double): Vector2d =
    val e = a.elements
    val rx = e(0) * v.x + e(1) * v.y + e(2) * z
    val ry = e(3) * v.x + e(4) * v.y + e(5) * z
    val rz = e(6) * v.x + e(7) * v.y + e(8) * z
    Vector2d(rx / rz, ry / rz)

  def rotate2d(a: Matrix3d, v: IVector2d): Vector2d =
    val e = a.elements
    val rx = e(0) * v.x + e(1) * v.y
    val ry = e(3) * v.x + e(4) * v.y
    Vector2d(rx, ry)

  private inline def elementWiseOperation(left: Matrix3d, right: Matrix3d, result: Matrix3d)(inline op: (Double, Double) => Double): Matrix3d =
    for (i <- FastRange(9)) {
      result.elements(i) = op(left.elements(i), right.elements(i))
    }
    result

  def add(a: Matrix3d, b: Matrix3d, result: Matrix3d): Matrix3d =
    elementWiseOperation(a, b, result)(_ + _)

  def sub(a: Matrix3d, b: Matrix3d, result: Matrix3d): Matrix3d =
    elementWiseOperation(a, b, result)(_ + _)

  def multiplyAdd(a: Matrix3d, b: Matrix3d, v: Double, result: Matrix3d): Matrix3d =
    elementWiseOperation(a, b, result) { (left, right) => left + right * v }

  inline def determinant(a00: Double, a01: Double, a02: Double,
                         a10: Double, a11: Double, a12: Double,
                         a20: Double, a21: Double, a22: Double): Double =
    a00 * (a11 * a22 - a21 * a12) +
      a01 * (a12 * a20 - a10 * a22) +
      a02 * (a10 * a21 - a11 * a20)

  def determinant(x: IVector3d, y: IVector3d, z: IVector3d): Double =
    determinant(x.x, x.y, x.z, y.x, y.y, y.z, z.x, z.y, z.z)

  def invertMatrix(a: Matrix3d, result: Matrix3d): Matrix3d =
    val det = a.det() // this may be 0.0, check if necessary
    val d = 1.0 / det
    val arr = a.elements

    inline def f(x: Int, y: Int) = arr(y * 3 + x)

    result := (
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