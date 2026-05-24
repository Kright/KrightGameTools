package me.kright.gametools.pga3d.geom

import me.kright.gametools.pga3d.{Pga3dPoint, Pga3dVector}

/**
 * March through all cells that could be visited by Ray casting with such an origin and direction.
 * Assume that the cell size is 1.0.
 */
final class Pga3dDigitalDifferentialAnalyzer(origin: Pga3dPoint,
                                             direction: Pga3dVector):

  // Delta time for moving one cell in each direction
  private val dtx: Double = if (direction.x != 0) 1.0 / direction.x.abs else Double.PositiveInfinity
  private val dty: Double = if (direction.y != 0) 1.0 / direction.y.abs else Double.PositiveInfinity
  private val dtz: Double = if (direction.z != 0) 1.0 / direction.z.abs else Double.PositiveInfinity

  // Time to next intersection with x, y, z planes
  private var tx: Double = calculateInitialT(origin.x, direction.x)
  private var ty: Double = calculateInitialT(origin.y, direction.y)
  private var tz: Double = calculateInitialT(origin.z, direction.z)

  // Step direction for each axis (-1 or 1)
  private val stepX: Int = if (direction.x > 0) 1 else -1
  private val stepY: Int = if (direction.y > 0) 1 else -1
  private val stepZ: Int = if (direction.z > 0) 1 else -1

  // current coordinates of cell
  var x: Int = origin.x.floor.toInt
  var y: Int = origin.y.floor.toInt
  var z: Int = origin.z.floor.toInt

  private def calculateInitialT(pos: Double, dir: Double): Double =
    if (dir == 0.0) Double.PositiveInfinity
    else
      val next = if (dir > 0.0) pos.floor + 1 else pos.floor
      (next - pos) / dir

  /**
   * update x or y or z by 1 or -1
   */
  def doStep(): Unit =
    if (tx <= ty && tx <= tz)
      x += stepX
      tx += dtx
    else if (ty <= tz)
      y += stepY
      ty += dty
    else
      z += stepZ
      tz += dtz
