package me.kright.gametools.pga2d.geom

import me.kright.gametools.flatarray.FlatDoubleSerializer
import me.kright.gametools.mathutil.CanEqualWithEps
import me.kright.gametools.pga2d.Pga2dPoint

/**
 * the 2d sibling of Pga3dSphere
 */
case class Pga2dCircle(center: Pga2dPoint,
                       r: Double) derives CanEqual, CanEqualWithEps, FlatDoubleSerializer:

  def toAABB: Pga2dAABB =
    Pga2dAABB(this)

  def expand(dr: Double): Pga2dCircle =
    Pga2dCircle(center, this.r + dr)

  def hasIntersection(s: Pga2dCircle): Boolean =
    val rSum = r + s.r
    (center - s.center).normSquare <= rSum * rSum

  def intersects(s: Pga2dCircle): Boolean =
    hasIntersection(s)

  def intersects(capsule: Pga2dCapsule): Boolean =
    capsule.intersects(this)

  def intersects(triangle: Pga2dTriangle): Boolean =
    triangle.distanceSquareTo(center) <= r * r
