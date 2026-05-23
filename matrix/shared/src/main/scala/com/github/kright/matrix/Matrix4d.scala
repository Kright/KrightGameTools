package com.github.kright.matrix

object Matrix4d:
  def zero: Matrix = Matrix(4, 4)

  def id: Matrix = Matrix4d(Array(
    1.0, 0.0, 0.0, 0.0,
    0.0, 1.0, 0.0, 0.0,
    0.0, 0.0, 1.0, 0.0,
    0.0, 0.0, 0.0, 1.0)
  )

  def apply(array: Array[Double]): Matrix =
    Matrix(4, 4, array)

  private def minor(m: Matrix, y1: Int, y2: Int, y3: Int, x1: Int, x2: Int, x3: Int): Double =
    Matrix3d.determinant(
      m(y1, x1), m(y1, x2), m(y1, x3),
      m(y2, x1), m(y2, x2), m(y2, x3),
      m(y3, x1), m(y3, x2), m(y3, x3),
    )

  private def minor(m: Matrix, yExclude: Int, xExclude: Int): Double =
    minor(m,
      if (0 < yExclude) 0 else 1,
      if (1 < yExclude) 1 else 2,
      if (2 < yExclude) 2 else 3,
      if (0 < xExclude) 0 else 1,
      if (1 < xExclude) 1 else 2,
      if (2 < xExclude) 2 else 3,
    )

  private def require4x4(m: Matrix): Unit = {
    require(m.w == 4)
    require(m.h == 4)
  }

  def determinant(m: Matrix): Double = {
    require4x4(m)

    val m00 = minor(m, 0, 0)
    val m01 = minor(m, 0, 1)
    val m02 = minor(m, 0, 2)
    val m03 = minor(m, 0, 3)

    m(0, 0) * m00 - m(0, 1) * m01 + m(0, 2) * m02 - m(0, 3) * m03
  }

  def inverted(m: Matrix): Matrix =
    require4x4(m)

    val m00 = minor(m, 0, 0)
    val m01 = minor(m, 0, 1)
    val m02 = minor(m, 0, 2)
    val m03 = minor(m, 0, 3)

    val det = m(0, 0) * m00 - m(0, 1) * m01 + m(0, 2) * m02 - m(0, 3) * m03
    val d = 1.0 / det

    Matrix(4, 4, Array(
      d * m00, -d * minor(m, 1, 0), d * minor(m, 2, 0), -d * minor(m, 3, 0),
      -d * m01, d * minor(m, 1, 1), -d * minor(m, 2, 1), d * minor(m, 3, 1),
      d * m02, -d * minor(m, 1, 2), d * minor(m, 2, 2), -d * minor(m, 3, 2),
      -d * m03, d * minor(m, 1, 3), -d * minor(m, 2, 3), d * minor(m, 3, 3),
    ))


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
  def projectionCamera(tanX: Double, tanY: Double, near: Double, far: Double): Matrix =
    // fit frustum into cube [-1, 1]
    val depth = far - near
    val sx = 1.0 / tanX
    val sy = 1.0 / tanY
    val sz = (near + far) / depth
    val sw = -2.0 * far * near / depth
    //@formatter:off
    Matrix4d(Array(
      sx, 0.0, 0.0, 0.0,
      0.0,  sy, 0.0, 0.0,
      0.0, 0.0,  sz,  sw,
      0.0, 0.0, 1.0, 0.0,
    ))
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
  def projectionCameraZ10(tanX: Double, tanY: Double, near: Double, far: Double): Matrix =
    val depth = far - near
    val sx = 1.0 / tanX
    val sy = 1.0 / tanY
    val sz = near / depth
    val sw = -far * near / depth
    //@formatter:off
    Matrix4d(Array(
      sx, 0.0, 0.0, 0.0,
      0.0,  sy, 0.0, 0.0,
      0.0, 0.0,  sz,  sw,
      0.0, 0.0, 1.0, 0.0,
    ))
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
  def projectionCameraZ10(tanX: Double, tanY: Double, near: Double): Matrix =
    val sx = 1.0 / tanX
    val sy = 1.0 / tanY
    val sw = -near
    //@formatter:off
    Matrix4d(Array(
      sx, 0.0, 0.0, 0.0,
      0.0,  sy, 0.0, 0.0,
      0.0, 0.0, 0.0,  sw,
      0.0, 0.0, 1.0, 0.0,
    ))
  //@formatter:on

//  def lookAt(from: IVector3d, to: IVector3d, up: IVector3d): Matrix4d =
//    val z = (to - from).normalize()
//    val x = up.cross(z).normalize()
//    val y = z.cross(x)
//    this := (
//      x.x, x.y, x.z, -from.dot(x),
//      y.x, y.y, y.z, -from.dot(y),
//      z.x, z.y, z.z, -from.dot(z),
//      0.0, 0.0, 0.0, 1.0,
//    )
