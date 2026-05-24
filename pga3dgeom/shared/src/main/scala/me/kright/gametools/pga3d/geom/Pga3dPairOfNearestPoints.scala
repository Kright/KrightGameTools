package me.kright.gametools.pga3d.geom

import me.kright.gametools.pga3d.Pga3dPoint


/**
 * Class for finding the pair of nearest points
 */
final class Pga3dPairOfNearestPoints(var a: Pga3dPoint,
                                     var b: Pga3dPoint):

  def this(pair: (Pga3dPoint, Pga3dPoint)) = this(pair._1, pair._2)

  var distSquare: Double = (a - b).normSquare

  def dist: Double =
    Math.sqrt(distSquare)

  def pair: (Pga3dPoint, Pga3dPoint) =
    (a, b)

  def update(a2: Pga3dPoint, b2: Pga3dPoint): Unit =
    val distSquare2 = (a2 - b2).normSquare
    if (distSquare2 < distSquare) {
      distSquare = distSquare2
      a = a2
      b = b2
    }

  def update(pair: (Pga3dPoint, Pga3dPoint)): Unit =
    update(pair._1, pair._2)
