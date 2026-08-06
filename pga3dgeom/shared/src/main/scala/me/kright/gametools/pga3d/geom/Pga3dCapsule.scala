package me.kright.gametools.pga3d.geom

import me.kright.gametools.flatarray.FlatDoubleSerializer
import me.kright.gametools.mathutil.CanEqualWithEps
import me.kright.gametools.pga3d.{Pga3dMotor, Pga3dPoint, Pga3dRotor, Pga3dTranslator, Pga3dVector}

/**
 * a capsule: all points within r of the segment [a, b], stored as the two hemisphere
 * centers plus the radius. Any combination of values is valid (no unit or normalization
 * invariants); a == b degenerates to a sphere.
 * The engine-style representation is available through [[Pga3dCapsule.fromCenter]]
 * and the [[center]] / [[halfAxis]] accessors
 */
case class Pga3dCapsule(a: Pga3dPoint,
                        b: Pga3dPoint,
                        r: Double) derives CanEqual, CanEqualWithEps, FlatDoubleSerializer:

  def edge: Pga3dEdge =
    Pga3dEdge(a, b)

  def center: Pga3dPoint =
    Pga3dPoint.mid(a, b)

  /** the vector from the center to the hemisphere center b: (b - a) / 2 */
  def halfAxis: Pga3dVector =
    (b - a) * 0.5

  def expand(dr: Double): Pga3dCapsule =
    Pga3dCapsule(a, b, r + dr)

  def map(f: Pga3dPoint => Pga3dPoint): Pga3dCapsule =
    Pga3dCapsule(f(a), f(b), r)

  def toAABB: Pga3dAABB =
    Pga3dAABB(this)

  def intersects(sphere: Pga3dSphere): Boolean =
    val rSum = r + sphere.r
    edge.distanceSquareTo(sphere.center) <= rSum * rSum

  def intersects(other: Pga3dCapsule): Boolean =
    val rSum = r + other.r
    edge.distanceSquareTo(other.edge) <= rSum * rSum

  def intersects(triangle: Pga3dTriangle): Boolean =
    triangle.distanceSquareTo(edge) <= r * r

  /**
   * the contact with the triangle, if any.
   * Non-piercing case (the axis does not cross the triangle): as for a sphere - the nearest
   * triangle point, the unit normal towards the nearest axis point, the depth `r - distance`.
   * Piercing case (the axis crosses the triangle interior): the contact point is the crossing,
   * the normal is the triangle plane normal oriented towards the larger part of the axis,
   * and the depth is `r + reach`, where reach is how far the axis extends beyond the plane
   * on the other side - so pushing by `depth` along the normal fully separates the capsule.
   * Note the depth is discontinuous when the axis slides off the triangle boundary,
   * inherent to a single-contact result.
   * As for the sphere, the pathological "axis touches a degenerate triangle exactly"
   * has no defined normal and returns null
   */
  def deepestContact(triangle: Pga3dTriangle): Pga3dContact | Null = {
    val (onTriangle, onEdge) = triangle.getNearestPoints(edge)
    val toAxis = onEdge - onTriangle
    val distSquare = toAxis.normSquare
    if (!(distSquare <= r * r)) return null // NaN input also lands here

    if (distSquare > 0.0) {
      val dist = Math.sqrt(distSquare)
      return Pga3dContact(onTriangle, toAxis * (1.0 / dist), r - dist)
    }

    // the axis pierces or touches the triangle: the normal comes from the plane,
    // oriented towards the larger part of the axis (computed geometrically, so the
    // orientation convention of the plane join does not matter)
    val plane = triangle.normalizedPlane
    val n = Pga3dVector(plane.x, plane.y, plane.z)
    if (!(n.normSquare > 0.5)) return null // degenerate triangle: no normal is defined

    val ha = n.antiDotI(a - onTriangle)
    val hb = n.antiDotI(b - onTriangle)
    val sign = if (ha + hb >= 0.0) 1.0 else -1.0
    // how far the axis reaches beyond the plane against the normal; zero when only touching
    val reach = Math.max(-Math.min(sign * ha, sign * hb), 0.0)
    Pga3dContact(onTriangle, n * sign, r + reach)
  }


object Pga3dCapsule:
  /**
   * engine-style construction: the center and the half axis (direction times half height).
   * The half axis is not required to be unit or non-zero: a zero half axis gives a sphere
   */
  def fromCenter(center: Pga3dPoint, halfAxis: Pga3dVector, r: Double): Pga3dCapsule =
    Pga3dCapsule(center - halfAxis, center + halfAxis, r)

  /** a sphere is the degenerate capsule with a == b */
  def apply(sphere: Pga3dSphere): Pga3dCapsule =
    Pga3dCapsule(sphere.center, sphere.center, sphere.r)

  extension (m: Pga3dMotor)
    def sandwich(capsule: Pga3dCapsule): Pga3dCapsule =
      capsule.map(m.sandwich(_).toPointUnsafe)

  extension (t: Pga3dTranslator)
    def sandwich(capsule: Pga3dCapsule): Pga3dCapsule =
      capsule.map(t.sandwich)

  extension (rotor: Pga3dRotor)
    def sandwich(capsule: Pga3dCapsule): Pga3dCapsule =
      capsule.map(rotor.sandwich(_).toPointUnsafe)
