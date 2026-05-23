package com.github.kright.math

import scala.math.Fractional.Implicits.infixFractionalOps

final case class Vector2d(x: Double,
                          y: Double) extends VectorNd[Vector2d]:

  override def +(v: Vector2d): Vector2d = Vector2d(x + v.x, y + v.y)

  override def -(v: Vector2d): Vector2d = Vector2d(x - v.x, y - v.y)

  override def *(v: Vector2d): Vector2d = Vector2d(x * v.x, y * v.y)

  override def *(m: Double): Vector2d = Vector2d(x * m, y * m)

  override def /(v: Vector2d): Vector2d = Vector2d(x / v.x, y / v.y)

  override def ^(pow: Double): Vector2d = Vector2d(Math.pow(x, pow), Math.pow(y, pow))

  override def ^(v: Vector2d): Vector2d = Vector2d(Math.pow(x, v.x), Math.pow(y, v.y))

  override infix def dot(v: Vector2d): Double =
    x * v.x + y * v.y

  def squareDistance(v: Vector2d): Double =
    val dx = x - v.x
    val dy = y - v.y
    dx * dx + dy * dy

  override def projected(axis: Vector2d): Vector2d =
    axis * (this.dot(axis) / axis.squareMag)

  override def min(v: Vector2d): Vector2d = getPerElement(v, Math.min)

  override def max(v: Vector2d): Vector2d = getPerElement(v, Math.max)

  override def clamp(lower: Vector2d, upper: Vector2d): Vector2d = this.max(lower).min(upper)

  private inline def getPerElement(v: Vector2d, inline f: (Double, Double) => Double): Vector2d =
    Vector2d(f(x, v.x), f(y, v.y))

  override def rejected(axis: Vector2d): Vector2d =
    this - projected(axis)

  override def isEquals(v: Vector2d, eps: Double): Boolean =
    Math.abs(x - v.x) <= eps && Math.abs(y - v.y) <= eps

  def sin(v: Vector2d): Double =
    (x * v.y - y * v.x) / Math.sqrt(squareMag * v.squareMag)

  override def toString: String = s"Vector2d($x, $y)"


object Vector2d:
  def apply(x: Double = 0.0, y: Double = 0.0): Vector2d =
    new Vector2d(x, y)

  def zero: Vector2d =
    new Vector2d(0.0, 0.0)
