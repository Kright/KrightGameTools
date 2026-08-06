package me.kright.gametools.pga3d.geom

import me.kright.gametools.pga3d.Pga3dPoint


/**
 * Class for finding the pair of nearest points
 */
final class Pga3dPairOfNearestPoints(var a: Pga3dPoint,
                                     var b: Pga3dPoint):

  def this(pair: (Pga3dPoint, Pga3dPoint)) = this(pair._1, pair._2)

  var distanceSquare: Double = (a - b).normSquare

  def distance: Double =
    Math.sqrt(distanceSquare)

  def pair: (Pga3dPoint, Pga3dPoint) =
    (a, b)

  /**
   * accepts the pair when it is strictly closer; returns true when the accumulator was
   * updated. NaN-safe in both directions: a NaN candidate never wins, and a NaN stored
   * distance is replaced by the first real candidate instead of blocking further updates
   */
  def update(a2: Pga3dPoint, b2: Pga3dPoint): Boolean =
    val distanceSquare2 = (a2 - b2).normSquare
    // !(a >= b) is "a < b or either is NaN"
    if (!(distanceSquare2 >= distanceSquare) && !distanceSquare2.isNaN) {
      distanceSquare = distanceSquare2
      a = a2
      b = b2
      true
    } else false

  def update(pair: (Pga3dPoint, Pga3dPoint)): Boolean =
    update(pair._1, pair._2)
