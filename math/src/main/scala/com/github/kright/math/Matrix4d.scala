package com.github.kright.math

import com.github.kright.math.MathUtil.swap


/**
 * indices in array:
 *
 * @formatter:off
 *  0  1  2  3
 *  4  5  6  7
 *  8  9 10 11
 * 12 13 14 15
 * @formatter:on
 */
final class Matrix4d(val elements: Array[Double]) extends MatrixNd[Matrix4d]:
  def this() = this(new Array[Double](16))

  inline def apply(y: Int, x: Int): Double = elements(x + y * 4)

  inline def update(y: Int, x: Int, value: Double): Unit = elements(x + y * 4) = value

  override def copy(): Matrix4d =
    new Matrix4d(elements.clone())

  override def setIdentity(): Matrix4d =
    this := (
      1.0, 0.0, 0.0, 0.0,
      0.0, 1.0, 0.0, 0.0,
      0.0, 0.0, 1.0, 0.0,
      0.0, 0.0, 0.0, 1.0
    )

  override def :=(m: Matrix4d): Matrix4d =
    System.arraycopy(m.elements, 0, elements, 0, 16)
    this

  // @formatter:off
  def :=(m: Matrix3d): Matrix4d =
    this := (
      m(0, 0), m(0, 1), m(0, 2), 0.0,
      m(1, 0), m(1, 1), m(1, 2), 0.0,
      m(2, 0), m(2, 1), m(2, 2), 0.0,
          0.0,     0.0,     0.0, 1.0
    )

  def :=(m: Matrix2d): Matrix4d =
    this := (
      m.m00, m.m01, 0.0, 0.0,
      m.m10, m.m11, 0.0, 0.0,
        0.0,   0.0, 1.0, 0.0,
        0.0,   0.0, 0.0, 1.0
    )

  def :=(q: IQuaternion): Matrix4d =
    this := (
      q.rotM00, q.rotM01, q.rotM02, 0.0,
      q.rotM10, q.rotM11, q.rotM12, 0.0,
      q.rotM20, q.rotM21, q.rotM22, 0.0,
           0.0,      0.0,      0.0, 1.0
    )

  def :=(euler: EulerAngles): Matrix4d =
    val cy = Math.cos(euler.yaw)
    val sy = Math.sin(euler.yaw)
    val cp = Math.cos(euler.pitch)
    val sp = Math.sin(euler.pitch)
    val cr = Math.cos(euler.roll)
    val sr = Math.sin(euler.roll)

    this := (
       cy * cr + sy * sp * sr, -cy * sr + sy * sp * cr, sy * cp, 0.0,
                      cp * sr,                 cp * cr,     -sp, 0.0,
      -sy * cr + cy * sp * sr,  sy * sr + cy * sp * cr, cy * cp, 0.0,
                          0.0,                     0.0,     0.0, 1.0,
    )


  def :=(a00: Double, a01: Double, a02: Double, a03: Double,
         a10: Double, a11: Double, a12: Double, a13: Double,
         a20: Double, a21: Double, a22: Double, a23: Double,
         a30: Double, a31: Double, a32: Double, a33: Double): Matrix4d =
    val e = elements
     e(0) = a00;  e(1) = a01;  e(2) = a02;  e(3) = a03
     e(4) = a10;  e(5) = a11;  e(6) = a12;  e(7) = a13
     e(8) = a20;  e(9) = a21; e(10) = a22; e(11) = a23
    e(12) = a30; e(13) = a31; e(14) = a32; e(15) = a33
    this
  // @formatter:on

  def setRow(y: Int, row: IVector4d): Matrix4d =
    this (y, 0) = row.x
    this (y, 1) = row.y
    this (y, 2) = row.z
    this (y, 3) = row.w
    this

  def setRows(r0: IVector4d, r1: IVector4d, r2: IVector4d, r3: IVector4d): Matrix4d =
    setRow(0, r0)
    setRow(1, r1)
    setRow(2, r2)
    setRow(3, r3)

  override def *(right: Matrix4d): Matrix4d =
    Matrix4d.multiply(this, right, new Matrix4d())

  override def *=(right: Matrix4d): Matrix4d =
    Matrix4d.multiply(this, right, result = this)

  override def *>(right: Matrix4d): Matrix4d =
    Matrix4d.multiply(this, right, result = right)

  override def *=(v: Double): Matrix4d =
    Matrix4d.multiply(this, v, result = this)

  override def *(v: Double): Matrix4d =
    Matrix4d.multiply(this, v, new Matrix4d())

  override def +=(m: Matrix4d): Matrix4d =
    Matrix4d.add(this, m, result = this)

  override def +(m: Matrix4d): Matrix4d =
    Matrix4d.add(this, m, result = new Matrix4d())

  override def -=(m: Matrix4d): Matrix4d =
    Matrix4d.sub(this, m, result = this)

  override def -(m: Matrix4d): Matrix4d =
    Matrix4d.sub(this, m, result = new Matrix4d())

  override def madd(m: Matrix4d, v: Double): Matrix4d =
    Matrix4d.multiplyAdd(this, m, v, result = this)


  def *(v: IVector4d): Vector4d =
    Matrix4d.multiply(this, v)

  def *>(v: Vector4d): Vector4d =
    Matrix4d.multiply(this, v)

  def *(v: IVector3d): Vector3d =
    Matrix4d.multiply(this, v, vw = 1.0)

  def *>(v: Vector3d): Vector3d =
    Matrix4d.multiply(this, v, vw = 1.0)

  def rotate(v: Vector3d): Vector3d =
    Matrix4d.rotate3d(this, v)

  def minor(y1: Int, y2: Int, y3: Int, x1: Int, x2: Int, x3: Int): Double =
    Matrix3d.determinant(
      this (y1, x1), this (y1, x2), this (y1, x3),
      this (y2, x1), this (y2, x2), this (y2, x3),
      this (y3, x1), this (y3, x2), this (y3, x3),
    )

  def minor(yExclude: Int, xExclude: Int): Double =
    minor(
      if (0 < yExclude) 0 else 1,
      if (1 < yExclude) 1 else 2,
      if (2 < yExclude) 2 else 3,
      if (0 < xExclude) 0 else 1,
      if (1 < xExclude) 1 else 2,
      if (2 < xExclude) 2 else 3,
    )

  def det3x3(): Double =
    minor(3, 3)

  override def det(): Double =
    // https://en.wikipedia.org/wiki/Laplace_expansion
    this (0, 0) * minor(0, 0) - this (0, 1) * minor(0, 1) +
      this (0, 2) * minor(0, 2) - this (0, 3) * minor(0, 3)

  override def invert(): Matrix4d =
    val m00 = minor(0, 0)
    val m01 = minor(0, 1)
    val m02 = minor(0, 2)
    val m03 = minor(0, 3)

    val det = this (0, 0) * m00 - this (0, 1) * m01 + this (0, 2) * m02 - this (0, 3) * m03
    val d = 1.0 / det

    this := (
      d * m00, -d * minor(1, 0), d * minor(2, 0), -d * minor(3, 0),
      -d * m01, d * minor(1, 1), -d * minor(2, 1), d * minor(3, 1),
      d * m02, -d * minor(1, 2), d * minor(2, 2), -d * minor(3, 2),
      -d * m03, d * minor(1, 3), -d * minor(2, 3), d * minor(3, 3),
    )

  override def transpose(): Matrix4d =
    val e = elements
    e.swap(1, 4)
    e.swap(2, 8)
    e.swap(3, 12)
    e.swap(6, 9)
    e.swap(7, 13)
    e.swap(11, 14)
    this

  def setTranslation(t: IVector3d): Matrix4d =
    this := (
      1.0, 0.0, 0.0, t.x,
      0.0, 1.0, 0.0, t.y,
      0.0, 0.0, 1.0, t.z,
      0.0, 0.0, 0.0, 1.0
    )

  def setScale(s: IVector3d): Matrix4d =
    this := (
      s.x, 0.0, 0.0, 0.0,
      0.0, s.y, 0.0, 0.0,
      0.0, 0.0, s.z, 0.0,
      0.0, 0.0, 0.0, 1.0
    )

  def setScale(s: IVector4d): Matrix4d =
    this := (
      s.x, 0.0, 0.0, 0.0,
      0.0, s.y, 0.0, 0.0,
      0.0, 0.0, s.z, 0.0,
      0.0, 0.0, 0.0, s.w
    )

  /**
   * camera orientation: depth along Z axis
   * x, y coordinates converted into perspective: x / z / tanX, y / z / tanY
   *
   * for flipping X or Y axis just pass negative tangent
   *
   * @param near - z coordinate of frustum place converted into z = -1
   * @param far  - z coordinate of frustum plane converted into z = 1
   * @param tanX - tangent of half of the horizontal pov, 1.0 for 45 degrees
   * @param tanY - tangent of half of the vertical pov, 1.0 for 45 degrees
   * @return self
   */
  def setProjectionCamera(tanX: Double, tanY: Double, near: Double, far: Double): Matrix4d =
    // fit frustum into cube [-1, 1]
    val depth = far - near
    val sx = 1.0 / tanX
    val sy = 1.0 / tanY
    val sz = (near + far) / depth
    val sw = -2.0 * far * near / depth
    //@formatter:off
    this := (
       sx, 0.0, 0.0, 0.0,
      0.0,  sy, 0.0, 0.0,
      0.0, 0.0,  sz,  sw,
      0.0, 0.0, 1.0, 0.0,
    )
    //@formatter:on


  /**
   * more precise way for depth at far plane
   *
   * camera orientation: along Z axis, z = near and z = far converted to z = -1 and z = 0
   * x, y coordinates converted into perspective: x / z / tanX, y / z / tanY
   *
   * for flipping X or Y axis just pass negative tangent
   *
   * @param tanX - tangent of half of the horizontal pov, 1.0 for 45 degrees
   * @param tanY - tangent of half of the vertical pov, 1.0 for 45 degrees
   * @param near - z coordinate of frustum plane converted into z = -1
   * @param far  - z coordinate of frustum plane converted into z = 0
   * @return self
   */
  def setProjectionCameraZ10(tanX: Double, tanY: Double, near: Double, far: Double): Matrix4d =
    val depth = far - near
    val sx = 1.0 / tanX
    val sy = 1.0 / tanY
    val sz = near / depth
    val sw = -far * near / depth
    //@formatter:off
    this := (
       sx, 0.0, 0.0, 0.0,
      0.0,  sy, 0.0, 0.0,
      0.0, 0.0,  sz,  sw,
      0.0, 0.0, 1.0, 0.0,
    )
    //@formatter:on

  /**
   * camera orientation: along Z axis, z = near and z = infinity converted to z = -1 and z = 0
   * x, y coordinates converted into perspective: x / z / tanX, y / z / tanY
   *
   * for flipping X or Y axis just pass negative tangent
   *
   * @param tanX - tangent of half of the horizontal pov, 1.0 for 45 degrees
   * @param tanY - tangent of half of the vertical pov, 1.0 for 45 degrees*
   * @param near - z coordinate of frustum plane converted into z = -1
   * @return self
   */
  def setProjectionCameraZ10(tanX: Double, tanY: Double, near: Double): Matrix4d =
    val sx = 1.0 / tanX
    val sy = 1.0 / tanY
    val sw = -near
    //@formatter:off
    this := (
       sx, 0.0, 0.0, 0.0,
      0.0,  sy, 0.0, 0.0,
      0.0, 0.0, 0.0,  sw,
      0.0, 0.0, 1.0, 0.0,
    )
    //@formatter:on

  def setLookAt(from: IVector3d, to: IVector3d, up: IVector3d): Matrix4d =
    val z = (to - from).normalize()
    val x = up.cross(z).normalize()
    val y = z.cross(x)
    this := (
      x.x, x.y, x.z, -from.dot(x),
      y.x, y.y, y.z, -from.dot(y),
      z.x, z.y, z.z, -from.dot(z),
      0.0, 0.0, 0.0, 1.0,
    )

  def setRotation(forwardZ: IVector3d, upY: IVector3d): Matrix4d =
    val z = forwardZ.normalized()
    val x = upY.cross(z).normalize()
    val y = z.cross(x)
    this := (
      x.x, x.y, x.z, 0.0,
      y.x, y.y, y.z, 0.0,
      z.x, z.y, z.z, 0.0,
      0.0, 0.0, 0.0, 1.0,
    )

  override def isEquals(other: Matrix4d, eps: Double): Boolean =
    MathUtil.isEquals(elements, other.elements, eps)

  override def toString: String =
    s"Matrix4d(${elements.mkString(", ")})"

object Matrix4d extends MatrixNdFactory[Matrix4d]:
  inline def apply() = new Matrix4d()

  override def zero: Matrix4d = new Matrix4d()

  override def id: Matrix4d = new Matrix4d().setIdentity()

  def multiply(a: Matrix4d, m: Double, result: Matrix4d): Matrix4d =
    for (i <- FastRange(16)) {
      result.elements(i) = a.elements(i) * m
    }
    result

  def multiply(left: Matrix4d, right: Matrix4d, result: Matrix4d): Matrix4d =
    inline def f(y: Int, x: Int) =
      left(y, 0) * right(0, x) + left(y, 1) * right(1, x) + left(y, 2) * right(2, x) + left(y, 3) * right(3, x)

    result := (
      f(0, 0), f(0, 1), f(0, 2), f(0, 3),
      f(1, 0), f(1, 1), f(1, 2), f(1, 3),
      f(2, 0), f(2, 1), f(2, 2), f(2, 3),
      f(3, 0), f(3, 1), f(3, 2), f(3, 3),
    )

  def multiply(matrix: Matrix4d, v: IVector4d): Vector4d =
    val e = matrix.elements

    inline def f(shift: Int): Double = e(shift) * v.x + e(shift + 1) * v.y + e(shift + 2) * v.z + e(shift + 3) * v.w

    Vector4d(f(0), f(4), f(8), f(12))

  def multiply(matrix: Matrix4d, v: IVector3d, vw: Double): Vector3d =
    val e = matrix.elements

    inline def f(shift: Int): Double = e(shift) * v.x + e(shift + 1) * v.y + e(shift + 2) * v.z + e(shift + 3) * vw

    val rx = f(0)
    val ry = f(4)
    val rz = f(8)
    val rw = f(12)
    val m = 1.0 / rw
    Vector3d(rx * m, ry * m, rz * m)

  def rotate3d(matrix: Matrix4d, v: IVector3d): Vector3d =
    val e = matrix.elements

    inline def f(shift: Int): Double = e(shift) * v.x + e(shift + 1) * v.y + e(shift + 2) * v.z

    Vector3d(f(0), f(4), f(8))

  private inline def elementWiseOperation(left: Matrix4d, right: Matrix4d, result: Matrix4d)(inline op: (Double, Double) => Double): Matrix4d =
    for (i <- FastRange(16)) {
      result.elements(i) = op(left.elements(i), right.elements(i))
    }
    result

  def add(a: Matrix4d, b: Matrix4d, result: Matrix4d): Matrix4d =
    elementWiseOperation(a, b, result)(_ + _)

  def sub(a: Matrix4d, b: Matrix4d, result: Matrix4d): Matrix4d =
    elementWiseOperation(a, b, result)(_ + _)

  def multiplyAdd(a: Matrix4d, b: Matrix4d, v: Double, result: Matrix4d): Matrix4d =
    elementWiseOperation(a, b, result) { (left, right) => left + right * v }