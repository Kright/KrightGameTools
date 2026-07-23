package me.kright.gametools.pga2d.geom

import me.kright.gametools.mathutil.MathUtil.clamp
import me.kright.gametools.pga2d.*
import me.kright.gametools.mathutil.MathUtil

case class Pga2dEdge(a: Pga2dPoint,
                     b: Pga2dPoint) derives CanEqual:

  override def toString: String = s"Pga2dEdge(a = $a, b = $b)"

  def line: Pga2dLine =
    a v b

  def interpolatedPoint(t: Double): Pga2dPoint =
    Pga2dPoint.interpolate(a, b, t)

  def getInterpolationFactor(point: Pga2dPoint): Double =
    Pga2dEdge.getInterpolationFactor(a, b, point)

  def center: Pga2dPoint =
    Pga2dPoint.mid(a, b)

  def direction: Pga2dVector =
    b - a

  def halfDirection: Pga2dVector =
    (b - a) * 0.5

  def normalizedDirection: Pga2dVector =
    direction.normalizedByNorm

  def magnitude: Double =
    (b - a).norm

  def magnitudeSquare: Double =
    (b - a).normSquare

  def map(f: Pga2dPoint => Pga2dPoint): Pga2dEdge =
    Pga2dEdge(f(a), f(b))

  def reversed: Pga2dEdge =
    Pga2dEdge(b, a)

  def vertices: Array[Pga2dPoint] =
    Array(a, b)

  def toAABB: Pga2dAABB =
    Pga2dAABB(this)

  def toRay: Pga2dRay =
    Pga2dRay(a, direction)

  def toRayNormalized: Pga2dRay =
    Pga2dRay(a, direction.normalizedByNorm)

  def distanceFromLineTo(p: Pga2dPoint): Double =
    val projected = p.projectOntoLine(line).toPoint
    (projected - p).norm

  def getNearestPoint(p: Pga2dPoint): Pga2dPoint =
    val t = getInterpolationFactor(p).clamp(0.0, 1.0)
    interpolatedPoint(t)

  def distanceTo(p: Pga2dPoint): Double =
    (getNearestPoint(p) - p).norm

  def contains(p: Pga2dPoint, eps: Double): Boolean =
    distanceTo(p) <= eps

  def getNearestPointsBinSearch(other: Pga2dEdge): (Pga2dPoint, Pga2dPoint) =
    Pga2dEdge.getNearestPointsBinSearch(this, other, 0.0, 1.0)

  def getNearestPoints(other: Pga2dEdge): (Pga2dPoint, Pga2dPoint) =
    Pga2dEdge.getNearestPoints(this, other)

  def distanceTo(other: Pga2dEdge): Double =
    val (p1, p2) = getNearestPoints(other)
    (p1 - p2).norm

  def intersects(other: Pga2dEdge, eps: Double): Boolean =
    distanceTo(other) <= eps


object Pga2dEdge:
  extension (p: Pga2dMotor)
    def sandwich(edge: Pga2dEdge): Pga2dEdge =
      edge.map(p.sandwich(_).toPointUnsafe)

  extension (p: Pga2dTranslator)
    def sandwich(edge: Pga2dEdge): Pga2dEdge =
      edge.map(p.sandwich)

  extension (p: Pga2dRotor)
    def sandwich(edge: Pga2dEdge): Pga2dEdge =
      edge.map(p.sandwich(_).toPointUnsafe)


  /**
   * Not effective, straightforward implementation with near perfect precision
   * Search distance between two edges by binary search.
   */
  def getNearestPointsBinSearch(a: Pga2dEdge, b: Pga2dEdge,
                                t0: Double = 0.0, t1: Double = 1.0): (Pga2dPoint, Pga2dPoint) = {
    val numberToDistance: Seq[(Int, Double)] = (0 to 4)
      .map { i =>
        val t = MathUtil.interpolate(t0, t1, i / 4.0)
        val ai = a.interpolatedPoint(t)
        val dist = b.distanceTo(ai)
        (i, dist)
      }

    val maxDist = numberToDistance.map(_._2).max
    val (minI, minDist) = numberToDistance.minBy(_._2)

    if ((maxDist - minDist) / minDist < 1e-14 || (t1 - t0) < 1e-14) {
      val (bestI, _) = numberToDistance.minBy(_._2)
      val t = MathUtil.interpolate(t0, t1, bestI / 4.0)
      val ap = a.interpolatedPoint(t)
      return (ap, b.getNearestPoint(ap))
    }

    getNearestPointsBinSearch(a, b,
      t0 = MathUtil.interpolate(t0, t1, Math.max(minI - 1, 0) / 4.0),
      t1 = MathUtil.interpolate(t0, t1, Math.min(minI + 1, 4) / 4.0),
    )
  }

  def getNearestPoints(self: Pga2dEdge, other: Pga2dEdge): (Pga2dPoint, Pga2dPoint) = {
    val mag2 = self.magnitudeSquare
    val otherMag2 = other.magnitudeSquare

    if (mag2 * 1e16 < otherMag2) {
      val center = self.center
      return (center, other.getNearestPoint(center))
    }

    if (otherMag2 * 1e16 < mag2) {
      val center = other.center
      return (self.getNearestPoint(center), center)
    }

    val u = self.direction
    val v = other.direction
    val w = self.a - other.a
    val uDotV = u antiDotI v
    val uDotW = u antiDotI w
    val vDotW = v antiDotI w
    val D = mag2 * otherMag2 - uDotV * uDotV

    var s: Double = 0.0
    var t: Double = 0.0

    if (D / (mag2 * otherMag2) < 1e-16) {
      // For parallel edges, we need to find the optimal s
      // We can use the projection of the vector between the starting points
      // onto the direction of the first edge
      s = (-uDotW / mag2).clamp(0.0, 1.0)
      t = ((uDotV * s + vDotW) / otherMag2).clamp(0.0, 1.0)
    } else {
      s = (uDotV * vDotW - otherMag2 * uDotW) / D
      t = (uDotV * s + vDotW) / otherMag2

      var needsAdjustment = false

      if (s < 0.0) {
        s = 0.0
        t = (vDotW / otherMag2).clamp(0.0, 1.0)
        needsAdjustment = true
      } else if (s > 1.0) {
        s = 1.0
        t = ((uDotV + vDotW) / otherMag2).clamp(0.0, 1.0)
        needsAdjustment = true
      }

      if (!needsAdjustment) {
        if (t < 0.0) {
          t = 0.0
          s = (-uDotW / mag2).clamp(0.0, 1.0)
        } else if (t > 1.0) {
          t = 1.0
          s = ((uDotV + vDotW - uDotW) / mag2).clamp(0.0, 1.0)
        }
      }
    }

    val searcher = new Pga2dPairOfNearestPoints(self.interpolatedPoint(s), other.interpolatedPoint(t))

    searcher.update(self.getNearestPoint(other.a), other.a)
    searcher.update(self.getNearestPoint(other.b), other.b)

    searcher.update(self.a, other.getNearestPoint(self.a))
    searcher.update(self.b, other.getNearestPoint(self.b))

    (searcher.a, searcher.b)
  }

  def getInterpolationFactor(a: Pga2dPoint, b: Pga2dPoint, point: Pga2dPoint): Double = {
    val pDir = point - a
    val ba = b - a
    ba.antiDotI(pDir) / ba.normSquare
  }
