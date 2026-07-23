package me.kright.gametools.pga2d.geom

import me.kright.gametools.pga2d.Pga2dPoint

final class Pga2dNearestPoint(val origin: Pga2dPoint,
                              var nearestPoint: Pga2dPoint | Null = null):
  var distSquare: Double =
    if (nearestPoint eq null) Double.PositiveInfinity
    else (origin - nearestPoint.nn).normSquare

  def distance: Double =
    Math.sqrt(distSquare)

  def update(newPoint: Pga2dPoint): Unit =
    val distSquare2 = (newPoint - origin).normSquare
    if (distSquare2 < distSquare) {
      distSquare = distSquare2
      nearestPoint = newPoint
    }
