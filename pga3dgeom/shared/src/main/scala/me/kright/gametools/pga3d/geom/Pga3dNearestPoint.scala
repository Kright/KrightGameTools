package me.kright.gametools.pga3d.geom

import me.kright.gametools.pga3d.Pga3dPoint

final class Pga3dNearestPoint(val origin: Pga3dPoint,
                              var nearestPoint: Pga3dPoint | Null = null):
  var distanceSquare: Double =
    if (nearestPoint eq null) Double.PositiveInfinity
    else (origin - nearestPoint.nn).normSquare

  def distance: Double =
    Math.sqrt(distanceSquare)

  /**
   * accepts newPoint when it is strictly closer to origin; returns true when the accumulator
   * was updated (e.g. to know which of several queried volumes produced the final contact).
   * NaN-safe in both directions: a NaN candidate never wins, and a NaN stored distance
   * is replaced by the first real candidate instead of blocking all further updates
   */
  def update(newPoint: Pga3dPoint): Boolean =
    val distanceSquare2 = (newPoint - origin).normSquare
    // !(a >= b) is "a < b or either is NaN"
    if (!(distanceSquare2 >= distanceSquare) && !distanceSquare2.isNaN) {
      distanceSquare = distanceSquare2
      nearestPoint = newPoint
      true
    } else false
