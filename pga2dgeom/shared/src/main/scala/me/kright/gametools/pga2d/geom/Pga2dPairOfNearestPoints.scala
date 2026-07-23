package me.kright.gametools.pga2d.geom

import me.kright.gametools.pga2d.Pga2dPoint


/**
 * Class for finding the pair of nearest points
 */
final class Pga2dPairOfNearestPoints(var a: Pga2dPoint,
                                     var b: Pga2dPoint):

  def this(pair: (Pga2dPoint, Pga2dPoint)) = this(pair._1, pair._2)

  var distSquare: Double = (a - b).normSquare

  def dist: Double =
    Math.sqrt(distSquare)

  def pair: (Pga2dPoint, Pga2dPoint) =
    (a, b)

  def update(a2: Pga2dPoint, b2: Pga2dPoint): Unit =
    val distSquare2 = (a2 - b2).normSquare
    if (distSquare2 < distSquare) {
      distSquare = distSquare2
      a = a2
      b = b2
    }

  def update(pair: (Pga2dPoint, Pga2dPoint)): Unit =
    update(pair._1, pair._2)
