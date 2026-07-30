package me.kright.gametools.pga2d.geom

import me.kright.gametools.flatarray.FlatDoubleSerializer
import me.kright.gametools.mathutil.CanEqualWithEps
import me.kright.gametools.pga2d.{Pga2dMotor, Pga2dPoint, Pga2dRotor, Pga2dTranslator, Pga2dVector}

/**
 * a 2d capsule (stadium): all points within r of the segment [a, b], stored as the two
 * cap centers plus the radius. Any combination of values is valid (no unit or
 * normalization invariants); a == b degenerates to a circle.
 * The engine-style representation is available through [[Pga2dCapsule.fromCenter]]
 * and the [[center]] / [[halfAxis]] accessors
 */
case class Pga2dCapsule(a: Pga2dPoint,
                        b: Pga2dPoint,
                        r: Double) derives CanEqual, CanEqualWithEps, FlatDoubleSerializer:

  def edge: Pga2dEdge =
    Pga2dEdge(a, b)

  def center: Pga2dPoint =
    Pga2dPoint.mid(a, b)

  /** the vector from the center to the cap center b: (b - a) / 2 */
  def halfAxis: Pga2dVector =
    (b - a) * 0.5

  def expand(dr: Double): Pga2dCapsule =
    Pga2dCapsule(a, b, r + dr)

  def map(f: Pga2dPoint => Pga2dPoint): Pga2dCapsule =
    Pga2dCapsule(f(a), f(b), r)

  def toAABB: Pga2dAABB =
    Pga2dAABB(this)

  def intersects(circle: Pga2dCircle): Boolean =
    val rSum = r + circle.r
    edge.distanceSquareTo(circle.center) <= rSum * rSum

  def intersects(other: Pga2dCapsule): Boolean =
    val rSum = r + other.r
    edge.distanceSquareTo(other.edge) <= rSum * rSum

  def intersects(triangle: Pga2dTriangle): Boolean =
    triangle.distanceSquareTo(edge) <= r * r


object Pga2dCapsule:
  /**
   * engine-style construction: the center and the half axis (direction times half height).
   * The half axis is not required to be unit or non-zero: a zero half axis gives a circle
   */
  def fromCenter(center: Pga2dPoint, halfAxis: Pga2dVector, r: Double): Pga2dCapsule =
    Pga2dCapsule(center - halfAxis, center + halfAxis, r)

  /** a circle is the degenerate capsule with a == b */
  def apply(circle: Pga2dCircle): Pga2dCapsule =
    Pga2dCapsule(circle.center, circle.center, circle.r)

  extension (m: Pga2dMotor)
    def sandwich(capsule: Pga2dCapsule): Pga2dCapsule =
      capsule.map(m.sandwich(_).toPointUnsafe)

  extension (t: Pga2dTranslator)
    def sandwich(capsule: Pga2dCapsule): Pga2dCapsule =
      capsule.map(t.sandwich)

  extension (rotor: Pga2dRotor)
    def sandwich(capsule: Pga2dCapsule): Pga2dCapsule =
      capsule.map(rotor.sandwich(_).toPointUnsafe)
