package me.kright.gametools.pga2d.geom

import me.kright.gametools.pga2d.{Pga2dPoint, Pga2dVector}

/**
 * March through all cells that could be visited by Ray casting with such an origin and direction.
 * Assume that the cell size is 1.0.
 */
final class Pga2dDigitalDifferentialAnalyzer(origin: Pga2dPoint,
                                             direction: Pga2dVector):

  // Delta time for moving one cell in each direction
  private val dtx: Double = if (direction.x != 0) 1.0 / direction.x.abs else Double.PositiveInfinity
  private val dty: Double = if (direction.y != 0) 1.0 / direction.y.abs else Double.PositiveInfinity

  // Time to next intersection with x, y planes
  private var tx: Double = calculateInitialT(origin.x, direction.x)
  private var ty: Double = calculateInitialT(origin.y, direction.y)

  // Step direction for each axis (-1 or 1)
  private val stepX: Int = if (direction.x > 0) 1 else -1
  private val stepY: Int = if (direction.y > 0) 1 else -1

  // current coordinates of cell
  var x: Int = origin.x.floor.toInt
  var y: Int = origin.y.floor.toInt

  private def calculateInitialT(pos: Double, dir: Double): Double =
    if (dir == 0.0) Double.PositiveInfinity
    else
      val next = if (dir > 0.0) pos.floor + 1 else pos.floor
      (next - pos) / dir

  /**
   * update x or y by 1 or -1
   */
  def doStep(): Unit =
    if (tx <= ty)
      x += stepX
      tx += dtx
    else
      y += stepY
      ty += dty
