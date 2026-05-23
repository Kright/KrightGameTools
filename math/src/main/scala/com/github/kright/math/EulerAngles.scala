package com.github.kright.math

import com.github.kright.matrix.{Matrix, Matrix3d}


/**
 * @param yaw   - angle in radians around axis Y (up)
 * @param pitch - angle in radians around axis X (right)
 * @param roll  - angle in radians around axis Z (forward)
 */
final case class EulerAngles(yaw: Double,
                             pitch: Double,
                             roll: Double) extends IEqualsWithEps[EulerAngles]:

  override def isEquals(e: EulerAngles, eps: Double): Boolean =
    Math.abs(yaw - e.yaw) < eps &&
      Math.abs(pitch - e.pitch) < eps &&
      Math.abs(roll - e.roll) < eps

  def toMatrix: Matrix =
    val cy = Math.cos(yaw)
    val sy = Math.sin(yaw)
    val cp = Math.cos(pitch)
    val sp = Math.sin(pitch)
    val cr = Math.cos(roll)
    val sr = Math.sin(roll)

    Matrix3d(Array(
      cy * cr + sy * sp * sr, -cy * sr + sy * sp * cr, sy * cp,
      cp * sr, cp * cr, -sp,
      -sy * cr + cy * sp * sr, sy * sr + cy * sp * cr, cy * cp,
    ))


  /** print angles in degrees */
  override def toString: String =
    inline def d(rads: Double) = f"${Math.toDegrees(rads)}%1.1f"

    f"EulerAngles(yaw=${d(yaw)}, pitch=${d(pitch)}, roll=${d(roll)})"

object EulerAngles:
  inline def apply(): EulerAngles = new EulerAngles(0.0, 0.0, 0.0)

  inline def fromDegrees(yaw: Double, pitch: Double, roll: Double): EulerAngles =
    new EulerAngles(
      Math.toRadians(yaw),
      Math.toRadians(pitch),
      Math.toRadians(roll),
    )

  def apply(m: Matrix): EulerAngles =
    restoreFromRotation(m(0, 0), m(0, 2), m(1, 0), m(1, 1), m(1, 2), m(2, 0), m(2, 2))

  def apply(q: Quaternion): EulerAngles =
    restoreFromRotation(q.rotM00, q.rotM02, q.rotM10, q.rotM11, q.rotM12, q.rotM20, q.rotM22)

  private inline def restoreFromRotation(inline m00: => Double,
                                         inline m02: => Double,
                                         inline m10: => Double,
                                         inline m11: => Double,
                                         inline m12: => Double,
                                         inline m20: => Double,
                                         inline m22: => Double): EulerAngles =
    val sp = -m12

    if (sp > 0.999999) {
      new EulerAngles(
        yaw = Math.atan2(-m20, m00),
        pitch = if (sp >= 1.0) 0.5 * Math.PI else Math.asin(sp),
        roll = 0.0
      )
    } else if (sp < -0.999999) {
      new EulerAngles(
        yaw = Math.atan2(-m20, m00),
        pitch = if (sp <= -1.0) -0.5 * Math.PI else Math.asin(sp),
        roll = 0.0
      )
    } else {
      new EulerAngles(
        yaw = Math.atan2(m02, m22),
        pitch = Math.asin(sp),
        roll = Math.atan2(m10, m11)
      )
    }
