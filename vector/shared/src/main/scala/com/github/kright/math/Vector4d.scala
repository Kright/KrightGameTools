package com.github.kright.math

import scala.math.Fractional.Implicits.infixFractionalOps

final case class Vector4d(x: Double,
                          y: Double,
                          z: Double,
                          w: Double) extends VectorNd[Vector4d]:


  override def +(v: Vector4d): Vector4d = Vector4d(x + v.x, y + v.y, z + v.z, w + v.w)

  override def -(v: Vector4d): Vector4d = Vector4d(x - v.x, y - v.y, z - v.z, w - v.w)

  override def *(v: Vector4d): Vector4d = Vector4d(x * v.x, y * v.y, z * v.z, w * v.w)

  override def *(m: Double): Vector4d = Vector4d(x * m, y * m, z * m, w * m)

  override def /(v: Vector4d): Vector4d = Vector4d(x / v.x, y / v.y, z / v.z, w / v.w)

  override def ^(pow: Double): Vector4d = Vector4d(Math.pow(x, pow), Math.pow(y, pow), Math.pow(z, pow), Math.pow(w, pow))

  override def ^(v: Vector4d): Vector4d = Vector4d(Math.pow(x, v.x), Math.pow(y, v.y), Math.pow(z, v.z), Math.pow(w, v.w))

  override infix def dot(v: Vector4d): Double =
    x * v.x + y * v.y + z * v.z + w * v.w

  override def projected(axis: Vector4d): Vector4d =
    axis * (this.dot(axis) / axis.squareMag)

  override def rejected(axis: Vector4d): Vector4d =
    this - projected(axis)

  override def min(v: Vector4d): Vector4d = getPerElement(v, Math.min)

  override def max(v: Vector4d): Vector4d = getPerElement(v, Math.max)

  override def clamp(lower: Vector4d, upper: Vector4d): Vector4d = this.max(lower).min(upper)

  private inline def getPerElement(v: Vector4d, inline f: (Double, Double) => Double): Vector4d =
    Vector4d(f(x, v.x), f(y, v.y), f(z, v.z), f(w, v.w))

  override def squareDistance(v: Vector4d): Double =
    val dx = x - v.x
    val dy = y - v.y
    val dz = z - v.z
    val dw = w - v.w
    dx * dx + dy * dy + dz * dz + dw * dw

  override def isEquals(v: Vector4d, eps: Double): Boolean =
    Math.abs(x - v.x) <= eps && Math.abs(y - v.y) <= eps && Math.abs(z - v.z) <= eps && Math.abs(w - v.w) <= eps

  override def toString: String = s"Vector4d($x, $y, $z, $w)"


object Vector4d:
  inline def apply(x: Double = 0.0, y: Double = 0.0, z: Double = 0.0, w: Double = 0.0): Vector4d =
    new Vector4d(x, y, z, w)

  def zero: Vector4d =
    new Vector4d(0.0, 0.0, 0.0, 0.0)
