package me.kright.gametools.pga2d.geom

import me.kright.gametools.pga2d.Pga2dPoint

/**
 * the 2d sibling of Pga3dSphere
 */
case class Pga2dCircle(center: Pga2dPoint,
                       r: Double):

  def toAABB: Pga2dAABB =
    Pga2dAABB(this)

  def expand(dr: Double): Pga2dCircle =
    Pga2dCircle(center, this.r + dr)

  def hasIntersection(s: Pga2dCircle): Boolean =
    val rSum = r + s.r
    (center - s.center).normSquare <= rSum * rSum
