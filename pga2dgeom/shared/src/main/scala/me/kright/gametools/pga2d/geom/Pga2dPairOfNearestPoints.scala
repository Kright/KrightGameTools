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

  /**
   * accepts the pair when it is strictly closer; returns true when the accumulator was
   * updated. NaN-safe in both directions: a NaN candidate never wins, and a NaN stored
   * distance is replaced by the first real candidate instead of blocking further updates
   */
  def update(a2: Pga2dPoint, b2: Pga2dPoint): Boolean =
    val distSquare2 = (a2 - b2).normSquare
    // !(a >= b) is "a < b or either is NaN"
    if (!(distSquare2 >= distSquare) && !distSquare2.isNaN) {
      distSquare = distSquare2
      a = a2
      b = b2
      true
    } else false

  def update(pair: (Pga2dPoint, Pga2dPoint)): Boolean =
    update(pair._1, pair._2)
