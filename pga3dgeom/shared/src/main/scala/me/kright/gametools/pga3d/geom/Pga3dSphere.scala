package me.kright.gametools.pga3d.geom

import me.kright.gametools.flatarray.FlatDoubleSerializer
import me.kright.gametools.mathutil.CanEqualWithEps
import me.kright.gametools.pga3d.{Pga3dPoint, Pga3dVector}

case class Pga3dSphere(center: Pga3dPoint,
                       r: Double) derives CanEqual, CanEqualWithEps, FlatDoubleSerializer:

  def toAABB: Pga3dAABB =
    Pga3dAABB(this)

  def expand(dr: Double): Pga3dSphere =
    Pga3dSphere(center, this.r + dr)

  def intersects(sphere: Pga3dSphere): Boolean =
    val rSum = r + sphere.r
    (center - sphere.center).normSquare <= rSum * rSum

  def intersects(capsule: Pga3dCapsule): Boolean =
    capsule.intersects(this)

  def intersects(triangle: Pga3dTriangle): Boolean =
    triangle.distanceSquareTo(center) <= r * r

  /**
   * the contact with the triangle, if any: the nearest point of the triangle to the sphere
   * center, the unit normal pointing from that point towards the center, and the
   * penetration depth `r - distance`.
   * When the center lies exactly on the triangle, the normal falls back to the triangle's
   * plane normal (its sign follows the vertex winding); for the pathological combination
   * "center exactly on a degenerate triangle" no normal is defined and None is returned
   */
  def deepestContact(triangle: Pga3dTriangle): Option[Pga3dContact] = {
    val nearest = triangle.getNearestPoint(center)
    val toCenter = center - nearest
    val distSquare = toCenter.normSquare
    if (!(distSquare <= r * r)) return None // NaN input also lands here

    if (distSquare > 0.0) {
      val dist = Math.sqrt(distSquare)
      Some(Pga3dContact(nearest, toCenter * (1.0 / dist), r - dist))
    } else {
      val plane = triangle.normalizedPlane
      val normal = Pga3dVector(plane.x, plane.y, plane.z)
      // a degenerate triangle has a NaN plane; NaN fails the check below
      if (normal.normSquare > 0.5) Some(Pga3dContact(nearest, normal, r)) else None
    }
  }
