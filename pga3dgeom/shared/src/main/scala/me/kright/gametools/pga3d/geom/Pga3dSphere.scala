package me.kright.gametools.pga3d.geom

import me.kright.gametools.pga3d.Pga3dPoint

case class Pga3dSphere(center: Pga3dPoint,
                       r: Double):

  def toAABB: Pga3dAABB =
    Pga3dAABB(this)

  def expand(dr: Double): Pga3dSphere =
    Pga3dSphere(center, this.r + dr)

  def hasIntersection(s: Pga3dSphere): Boolean =
    val rSum = r + s.r
    (center - s.center).normSquare <= rSum * rSum
