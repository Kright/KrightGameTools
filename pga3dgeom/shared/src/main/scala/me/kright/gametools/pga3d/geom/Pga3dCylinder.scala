package me.kright.gametools.pga3d.geom

import me.kright.gametools.flatarray.FlatDoubleSerializer
import me.kright.gametools.mathutil.CanEqualWithEps
import me.kright.gametools.pga3d.{Pga3dBivector, Pga3dMotor, Pga3dPoint, Pga3dRotor, Pga3dTranslator, Pga3dVector}

/**
 * a cylinder: all points within r of the axis segment [a, b], cut by the two flat cap planes
 * through a and b (unlike [[Pga3dCapsule]] there are no hemispherical caps). Any combination
 * of values is valid (no unit or normalization invariants); a == b degenerates to a flat disk.
 * The engine-style representation is available through [[Pga3dCylinder.fromCenter]]
 * and the [[center]] / [[halfAxis]] accessors
 */
case class Pga3dCylinder(a: Pga3dPoint,
                         b: Pga3dPoint,
                         r: Double) derives CanEqual, CanEqualWithEps, FlatDoubleSerializer:

  def edge: Pga3dEdge =
    Pga3dEdge(a, b)

  def center: Pga3dPoint =
    Pga3dPoint.mid(a, b)

  /** the vector from the center to the cap center b: (b - a) / 2 */
  def halfAxis: Pga3dVector =
    (b - a) * 0.5

  /** the axis as an infinite line */
  def line: Pga3dBivector =
    a v b

  /** widening: grows the radius only, like [[Pga3dSphere.expand]] and [[Pga3dCapsule.expand]] */
  def expand(dr: Double): Pga3dCylinder =
    Pga3dCylinder(a, b, r + dr)

  /** lengthening: moves both caps outwards along the axis by d */
  def expandAxis(d: Double): Pga3dCylinder =
    val shift = (b - a).normalizedByNorm * d
    Pga3dCylinder(a - shift, b + shift, r)

  def map(f: Pga3dPoint => Pga3dPoint): Pga3dCylinder =
    Pga3dCylinder(f(a), f(b), r)

  def toAABB: Pga3dAABB =
    Pga3dAABB(this)

  def boundingSphere: Pga3dSphere =
    Pga3dSphere(center, Math.sqrt(r * r + halfAxis.normSquare))

  def contains(point: Pga3dPoint): Boolean =
    val t = Pga3dEdge.getInterpolationFactor(a, b, point)
    if (t < 0.0 || t > 1.0) return false

    val pointOnAxis = point.projectOntoLine(line).toPoint
    (pointOnAxis - point).normSquare <= r * r

  def contains(edge: Pga3dEdge): Boolean =
    contains(edge.a) && contains(edge.b)

  def intersects(edge: Pga3dEdge): Boolean = {
    val ta = Pga3dEdge.getInterpolationFactor(a, b, edge.a)
    val tb = Pga3dEdge.getInterpolationFactor(a, b, edge.b)

    val taIsInside = 0.0 <= ta && ta <= 1.0
    val tbIsInside = 0.0 <= tb && tb <= 1.0

    if (taIsInside && edge.a.projectOntoLine(line).toPoint.distanceTo(edge.a) <= r) return true
    if (tbIsInside && edge.b.projectOntoLine(line).toPoint.distanceTo(edge.b) <= r) return true

    if (taIsInside && tbIsInside) {
      return Pga3dEdge(a, b).distanceTo(edge) <= r
    }

    if (ta < 0.0 && tb < 0.0) return false
    if (ta > 1.0 && tb > 1.0) return false

    // the interpolation factors along the edge where it crosses the cap planes t = 0 and t = 1;
    // an endpoint outside the axis range [0, 1] is replaced by the corresponding crossing, so the
    // clamped sub-segment projects entirely into the axis range
    val sAtT0 = -ta / (tb - ta)
    val sAtT1 = (1.0 - ta) / (tb - ta)

    val edgeClampedTa =
      if (ta < 0.0) edge.interpolatedPoint(sAtT0)
      else if (ta > 1.0) edge.interpolatedPoint(sAtT1)
      else edge.a

    val edgeClampedTb =
      if (tb < 0.0) edge.interpolatedPoint(sAtT0)
      else if (tb > 1.0) edge.interpolatedPoint(sAtT1)
      else edge.b

    Pga3dEdge(a, b).distanceTo(Pga3dEdge(edgeClampedTa, edgeClampedTb)) <= r
  }


object Pga3dCylinder:
  /**
   * engine-style construction: the center and the half axis (direction times half height).
   * The half axis is not required to be unit or non-zero: a zero half axis gives a flat disk
   */
  def fromCenter(center: Pga3dPoint, halfAxis: Pga3dVector, r: Double): Pga3dCylinder =
    Pga3dCylinder(center - halfAxis, center + halfAxis, r)

  extension (m: Pga3dMotor)
    def sandwich(cylinder: Pga3dCylinder): Pga3dCylinder =
      cylinder.map(m.sandwich(_).toPointUnsafe)

  extension (t: Pga3dTranslator)
    def sandwich(cylinder: Pga3dCylinder): Pga3dCylinder =
      cylinder.map(t.sandwich)

  extension (rotor: Pga3dRotor)
    def sandwich(cylinder: Pga3dCylinder): Pga3dCylinder =
      cylinder.map(rotor.sandwich(_).toPointUnsafe)
