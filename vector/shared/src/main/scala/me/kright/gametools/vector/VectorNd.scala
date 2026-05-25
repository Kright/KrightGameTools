package me.kright.gametools.vector

import me.kright.gametools.mathutil.IEqualsWithEps

trait VectorNd[Vec <: VectorNd[Vec]] extends IEqualsWithEps[Vec]:
  self: Vec =>

  def +(v: Vec): Vec

  def -(v: Vec): Vec

  def *(v: Vec): Vec

  def *(m: Double): Vec

  def /(v: Vec): Vec

  def /(d: Double): Vec = this * (1.0 / d)

  def ^(pow: Double): Vec

  def ^(v: Vec): Vec

  def unary_- : Vec = this * (-1)

  def min(v: Vec): Vec

  def max(v: Vec): Vec

  def clamp(lower: Vec, upper: Vec): Vec

  infix def dot(v: Vec): Double

  def cos(v: Vec): Double = this.dot(v) / Math.sqrt(this.squareMag * v.squareMag)

  def projected(axis: Vec): Vec

  def rejected(axis: Vec): Vec

  def decomposed(axis: Vec): (Vec, Vec) = (projected(axis), rejected(axis))

  def normalized: Vec = this / this.mag

  def squareMag: Double = this.dot(this)

  def mag: Double = Math.sqrt(squareMag)

  def squareDistance(v: Vec): Double

  def distance(v: Vec): Double = Math.sqrt(squareDistance(v))

  def isEquals(v: Vec, eps: Double): Boolean


object VectorNd:
  extension (m: Double)
    inline def *[Vec <: VectorNd[Vec]](v: VectorNd[Vec]): Vec =
      v * m
