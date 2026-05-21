package com.github.kright.math


/**
 * I used the book:
 * F. Dunn, I. Parberry - 3D Math Primer for Graphics and Game Development
 */
final case class Quaternion(w: Double,
                            x: Double,
                            y: Double,
                            z: Double) extends IEqualsWithEps[Quaternion]:

  def copy(): Quaternion =
    Quaternion(w, x, y, z)

  def conjugated(): Quaternion =
    Quaternion(w, -x, -y, -z)

  def unary_- : Quaternion =
    Quaternion(-w, -x, -y, -z)

  def normalized(): Quaternion =
    val selfMag = mag
    // minimal double value is 2.2250738585072014E-308
    if (selfMag < 1e-150) {
      Quaternion.id
    } else {
      this * (1.0 / selfMag)
    }

  def +(q: Quaternion): Quaternion =
    Quaternion(w + q.w, x + q.x, y + q.y, z + q.z)

  def -(q: Quaternion): Quaternion =
    Quaternion(w - q.w, x - q.x, y - q.y, z - q.z)

  def *(q: Quaternion): Quaternion =
    Quaternion.multiply(this, q)

  def *(v: IVector3d): Vector3d =
    Quaternion.multiply(this, v)

  def *(m: Double): Quaternion =
    Quaternion(this.w * m, this.x * m, this.y * m, this.z * m)

  def /(d: Double): Quaternion =
    this * (1.0 / d)

  def ^(pow: Double): Quaternion =
    val xyz = magXYZ
    if (xyz < 0.000001) return this.copy() // no rotation
    val alpha = pow * Math.atan2(xyz, w)
    val m = Math.sin(alpha) / xyz
    Quaternion(Math.cos(alpha), x * m, y * m, z * m)

  def squareMag: Double =
    w * w + x * x + y * y + z * z

  def mag: Double =
    Math.sqrt(w * w + x * x + y * y + z * z)

  def magXYZ: Double =
    Math.sqrt(x * x + y * y + z * z)

  def dot(q: Quaternion): Double =
    w * q.w + x * q.x + y * q.y + z * q.z

  def cos(q: Quaternion): Double =
    dot(q) / Math.sqrt(squareMag * q.squareMag)

  /** rotation axis with length of 0.5*angle in radians */
  def log: Vector3d =
    val xyz = magXYZ
    if (xyz < 1e-12) return Vector3d(0, 0, 0)
    val angle2 = Math.atan2(xyz, w)
    val mul = angle2 / xyz
    Vector3d(x * mul, y * mul, z * mul)

  def rotationAngleRadians(): Double =
    2.0 * Math.acos(w / mag)

  def rotationAngleRadiansForNormalized(): Double =
    2.0 * Math.acos(w)

  def rotationAxis: Vector3d =
    val div = magXYZ
    if (div > 0.000001) {
      val m = 1.0 / div
      Vector3d(x * m, y * m, z * m)
    } else { // no rotation
      Vector3d(1, 0, 0)
    }

  def oX: Vector3d =
    Vector3d(rotM00, rotM01, rotM02)

  def oY: Vector3d =
    Vector3d(rotM10, rotM11, rotM12)

  def oZ: Vector3d =
    Vector3d(rotM20, rotM21, rotM22)

  def isEquals(q: Quaternion, eps: Double): Boolean =
    inline def eq(a: Double, b: Double) = Math.abs(a - b) < eps

    if (eq(w, q.w)) {
      eq(x, q.x) && eq(y, q.y) && eq(z, q.z)
    } else {
      eq(w, -q.w) && eq(x, -q.x) && eq(y, -q.y) && eq(z, -q.z)
    }

  def toPrettyString: String =
    f"Quaternion(w=$w%1.3f, x=$x%1.3f, y=$y%1.3f, z=$z%1.3f)"

  // @formatter:off
  /* Quaternion as matrix */
  def rotM00: Double = 1.0 - 2.0f * (y * y + z * z)
  def rotM01: Double = 2.0 * (x * y - w * z)
  def rotM02: Double = 2.0 * (x * z + w * y)

  def rotM10: Double = 2.0 * (x * y + w * z)
  def rotM11: Double = 1.0 - 2.0f * (x * x + z * z)
  def rotM12: Double = 2.0 * (y * z - w * x)

  def rotM20: Double = 2.0 * (x * z - w * y)
  def rotM21: Double = 2.0 * (y * z + w * x)
  def rotM22: Double = 1.0 - 2.0 * (x * x + y * y)

  /* Names in terms of geometric algebra */
  def scalar: Double = w

  def xy: Double = z
  def yz: Double = x
  def zx: Double = y
  // @formatter:on

  override def toString: String =
    f"Quaternion(w=$w, x=$x, y=$y, z=$z)"


object Quaternion:
  extension (m: Double)
    inline def *(q: Quaternion): Quaternion = q * m
  
  val zero: Quaternion = new Quaternion(0.0, 0.0, 0.0, 0.0)

  val id: Quaternion = new Quaternion(1.0, 0.0, 0.0, 0.0)
  
  def apply(xyz: IVector3d) = new Quaternion(0.0, xyz.x, xyz.y, xyz.z)

  def apply(euler: EulerAngles): Quaternion =
    val cy = Math.cos(0.5 * euler.yaw)
    val sy = Math.sin(0.5 * euler.yaw)
    val cp = Math.cos(0.5 * euler.pitch)
    val sp = Math.sin(0.5 * euler.pitch)
    val cr = Math.cos(0.5 * euler.roll)
    val sr = Math.sin(0.5 * euler.roll)

    // yaw   axis Z
    // pitch axis Y
    // roll  axis X

    Quaternion(
      cr * cp * cy + sr * sp * sy, // w
      cr * sp * cy + sr * cp * sy, // y
      cr * cp * sy - sr * sp * cy, // z
      sr * cp * cy - cr * sp * sy, // x
    )

  def apply(angleRadians: Double, axis: IVector3d): Quaternion =
    val realAngle = angleRadians * 0.5
    val m = Math.sin(realAngle) / axis.mag
    Quaternion(Math.cos(realAngle), axis.x * m, axis.y * m, axis.z * m)

  // 16 multiplications, 12 additions
  def multiply(first: Quaternion, second: Quaternion): Quaternion =
    Quaternion(
      first.w * second.w - first.x * second.x - first.y * second.y - first.z * second.z,
      first.w * second.x + first.x * second.w + first.y * second.z - first.z * second.y,
      first.w * second.y + first.y * second.w + first.z * second.x - first.x * second.z,
      first.w * second.z + first.z * second.w + first.x * second.y - first.y * second.x
    )

  def mix(q1: Quaternion, k1: Double, q2: Quaternion, k2: Double): Quaternion =
    Quaternion(
      q1.w * k1 + q2.w * k2,
      q1.x * k1 + q2.x * k2,
      q1.y * k1 + q2.y * k2,
      q1.z * k1 + q2.z * k2
    )

  /**
   * spherical linear interpolation.
   * works only for normalized quaternions
   */
  def slerp(q1: Quaternion, q2: Quaternion, t: Double): Quaternion =
    if (t < 0) {
      return q1.copy()
    }
    if (t > 1) {
      return q2.copy()
    }
    var cosOmega = q1.dot(q2)

    val q2Fixed =
      if (cosOmega < 0.0) {
        cosOmega = -cosOmega
        -q2
      } else {
        q2
      }

    val linearCriteria = 0.9999f
    //if angle small, cosine is big, and we use linear interpolation
    if (cosOmega > linearCriteria) {
      return mix(q1, 1 - t, q2Fixed, t)
    }
    //copy-paste code from page 213
    val sinOmega = Math.sqrt(1.0 - cosOmega * cosOmega)
    val omega = Math.atan2(sinOmega, cosOmega)
    val mult = 1.0 / sinOmega
    val k1 = Math.sin((1.0 - t) * omega) * mult
    val k2 = Math.sin(t * omega) * mult
    mix(q1, k1, q2Fixed, k2)

  /**
   * linear interpolation. Really faster then spherical
   */
  def lerp(q1: Quaternion, q2: Quaternion, t: Float): Quaternion =
    if (q1.dot(q2) >= 0) {
      mix(q1, 1 - t, q2, t)
    } else {
      mix(q1, 1 - t, q2, -t)
    }

  def multiply(q: Quaternion, v: IVector3d): Vector3d =
    // val r = q * Quaternion(0.0, v.x, v.y, v.z) * q.conjugated()
    // result := (r.x, r.y, r.z)
    Vector3d(
      v.x * q.rotM00 + v.y * q.rotM01 + v.z * q.rotM02,
      v.x * q.rotM10 + v.y * q.rotM11 + v.z * q.rotM12,
      v.x * q.rotM20 + v.y * q.rotM21 + v.z * q.rotM22,
    )

  def restoreFromRotation(m: Matrix3d): Quaternion =
    val m00 = m(0, 0)
    val m11 = m(1, 1)
    val m22 = m(2, 2)
    val tr = m00 + m11 + m22
    val max = Math.max(tr, Math.max(m00, Math.max(m11, m22)))
    if (tr == max) {
      val w = Math.sqrt(1.0 + m00 + m11 + m22)
      return Quaternion(
        0.5 * w,
        0.5 * (m(2, 1) - m(1, 2)) / w,
        0.5 * (m(0, 2) - m(2, 0)) / w,
        0.5 * (m(1, 0) - m(0, 1)) / w,
      )
    }
    if (m00 == max) {
      val d = Math.sqrt(1.0 + m00 - m11 - m22)
      return Quaternion(
        0.5 * (m(2, 1) - m(1, 2)) / d,
        0.5 * d,
        0.5 * (m(0, 1) + m(1, 0)) / d,
        0.5 * (m(2, 0) + m(0, 2)) / d,
      )
    }
    if (m11 == max) {
      val d = Math.sqrt(1.0 - m00 + m11 - m22)
      return Quaternion(
        0.5 * (m(0, 2) - m(2, 0)) / d,
        0.5 * (m(0, 1) + m(1, 0)) / d,
        0.5 * d,
        0.5 * (m(1, 2) + m(2, 1)) / d
      )
    }
    // qz2 == max
    {
      val d = Math.sqrt(1.0 - m00 - m11 + m22)
      return Quaternion(
        0.5 * (m(1, 0) - m(0, 1)) / d,
        0.5 * (m(2, 0) + m(0, 2)) / d,
        0.5 * (m(2, 1) + m(1, 2)) / d,
        0.5 * d
      )
    }

  def restoreFromRotation(m: Matrix4d): Quaternion =
    val m00 = m(0, 0)
    val m11 = m(1, 1)
    val m22 = m(2, 2)
    val tr = m00 + m11 + m22
    val max = Math.max(tr, Math.max(m00, Math.max(m11, m22)))
    if (tr == max) {
      val w = Math.sqrt(1.0 + m00 + m11 + m22)
      return Quaternion(
        0.5 * w,
        0.5 * (m(2, 1) - m(1, 2)) / w,
        0.5 * (m(0, 2) - m(2, 0)) / w,
        0.5 * (m(1, 0) - m(0, 1)) / w,
      )
    }
    if (m00 == max) {
      val d = Math.sqrt(1.0 + m00 - m11 - m22)
      return Quaternion(
        0.5 * (m(2, 1) - m(1, 2)) / d,
        0.5 * d,
        0.5 * (m(0, 1) + m(1, 0)) / d,
        0.5 * (m(2, 0) + m(0, 2)) / d,
      )
    }
    if (m11 == max) {
      val d = Math.sqrt(1.0 - m00 + m11 - m22)
      return Quaternion(
        0.5 * (m(0, 2) - m(2, 0)) / d,
        0.5 * (m(0, 1) + m(1, 0)) / d,
        0.5 * d,
        0.5 * (m(1, 2) + m(2, 1)) / d
      )
    }
    // qz2 == max
    {
      val d = Math.sqrt(1.0 - m00 - m11 + m22)
      return Quaternion(
        0.5 * (m(1, 0) - m(0, 1)) / d,
        0.5 * (m(2, 0) + m(0, 2)) / d,
        0.5 * (m(2, 1) + m(1, 2)) / d,
        0.5 * d
      )
    }

  /**
   * @param sourceAxis : normalized vector
   * @param targetAxis : normalized vector
   * @param result     Quaternion for which q * sourceAxis == targetAxis
   * @return result
   */
  def fromAxisToAxis(sourceAxis: IVector3d, targetAxis: IVector3d): Quaternion =
    val mid = sourceAxis + targetAxis
    if (mid.mag < 1e-12) {
      // choose any vector perpendicular to sourceAxis
      mid := (1, 0, 0)
      val cos = mid.dot(sourceAxis)
      if (cos > -0.1 && cos < 0.1) {
        mid := (0, 1, 0)
      }
      mid.rejected(sourceAxis)
    }
    mid.normalize()
    fromAxisOverBisection(sourceAxis, mid)

  /**
   * @param sourceAxis : normalized vector
   * @param bisection  : normalized bisection between fromAxis and targetAxis
   * @param result     Quaternion for which q * sourceAxis == targetAxis
   * @return result
   */
  def fromAxisOverBisection(sourceAxis: IVector3d, bisection: IVector3d): Quaternion =
    val w = sourceAxis.dot(bisection)
    val xyz = sourceAxis.cross(bisection)
    Quaternion(w, xyz.x, xyz.y, xyz.z)

  /** exponent length is 0.5*angle of rotation */
  def fromExp(exp: IVector3d): Quaternion =
    val l = exp.mag

    // for small numerbs, sin(l) / l => (l - l^3 / 3! + ...) / l = 1 - l^2 / 6
    // double has 15-17 significant digits, so 1e-9 is small enough
    val m =
      if (math.abs(l) > 1e-9) Math.sin(l) / l
      else 1.0

    Quaternion(Math.cos(l), exp.x * m, exp.y * m, exp.z * m)  
