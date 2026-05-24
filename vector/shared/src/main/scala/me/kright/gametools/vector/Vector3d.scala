package me.kright.gametools.vector

final case class Vector3d(x: Double,
                          y: Double,
                          z: Double) extends VectorNd[Vector3d]:

  override def +(v: Vector3d): Vector3d = Vector3d(x + v.x, y + v.y, z + v.z)

  override def -(v: Vector3d): Vector3d = Vector3d(x - v.x, y - v.y, z - v.z)

  override def *(v: Vector3d): Vector3d = Vector3d(x * v.x, y * v.y, z * v.z)

  override def *(m: Double): Vector3d = Vector3d(x * m, y * m, z * m)

  override def /(v: Vector3d): Vector3d = Vector3d(x / v.x, y / v.y, z / v.z)

  override def ^(pow: Double): Vector3d = Vector3d(Math.pow(x, pow), Math.pow(y, pow), Math.pow(z, pow))

  override def ^(v: Vector3d): Vector3d = Vector3d(Math.pow(x, v.x), Math.pow(y, v.y), Math.pow(z, v.z))

  override infix def dot(v: Vector3d): Double =
    x * v.x + y * v.y + z * v.z

  override def squareDistance(v: Vector3d): Double =
    val dx = x - v.x
    val dy = y - v.y
    val dz = z - v.z
    dx * dx + dy * dy + dz * dz

  def cross(v: Vector3d): Vector3d =
    new Vector3d(
      y * v.z - z * v.y,
      z * v.x - x * v.z,
      x * v.y - y * v.x
    )

  override def projected(axis: Vector3d): Vector3d =
    axis * (this.dot(axis) / axis.squareMag)

  override def rejected(axis: Vector3d): Vector3d =
    this - projected(axis)

  override def min(v: Vector3d): Vector3d = getPerElement(v, Math.min)

  override def max(v: Vector3d): Vector3d = getPerElement(v, Math.max)

  override def clamp(lower: Vector3d, upper: Vector3d): Vector3d = this.max(lower).min(upper)

  private inline def getPerElement(v: Vector3d, inline f: (Double, Double) => Double): Vector3d =
    Vector3d(f(x, v.x), f(y, v.y), f(z, v.z))

  def sin(v: Vector3d): Double =
    Math.sqrt(cross(v).squareMag / (squareMag * v.squareMag))

  override def isEquals(v: Vector3d, eps: Double): Boolean =
    Math.abs(x - v.x) <= eps && Math.abs(y - v.y) <= eps && Math.abs(z - v.z) <= eps

  override def toString: String = s"Vector3d($x, $y, $z)"


object Vector3d:
  inline def apply(x: Double = 0.0, y: Double = 0.0, z: Double = 0.0): Vector3d =
    new Vector3d(x, y, z)

  def zero: Vector3d =
    new Vector3d(0.0, 0.0, 0.0)
