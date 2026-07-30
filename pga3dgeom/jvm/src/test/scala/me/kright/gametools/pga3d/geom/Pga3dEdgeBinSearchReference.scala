package me.kright.gametools.pga3d.geom

import me.kright.gametools.mathutil.MathUtil
import me.kright.gametools.pga3d.Pga3dPoint

/**
 * former Pga3dEdge.getNearestPointsBinSearch, removed from the library (slow, allocating,
 * and easy to call by accident instead of the analytic getNearestPoints);
 * kept here as a near-perfect-precision reference for tests
 */
object Pga3dEdgeBinSearchReference:
  def getNearestPoints(a: Pga3dEdge, b: Pga3dEdge,
                       t0: Double = 0.0, t1: Double = 1.0): (Pga3dPoint, Pga3dPoint) = {
    val numberToDistance: Seq[(Int, Double)] = (0 to 4)
      .map { i =>
        val t = MathUtil.interpolate(t0, t1, i / 4.0)
        val ai = a.interpolatedPoint(t)
        val dist = b.distanceTo(ai)
        (i, dist)
      }

    val maxDist = numberToDistance.map(_._2).max
    val (minI, minDist) = numberToDistance.minBy(_._2)

    if ((maxDist - minDist) / minDist < 1e-14 || (t1 - t0) < 1e-14) {
      val (bestI, _) = numberToDistance.minBy(_._2)
      val t = MathUtil.interpolate(t0, t1, bestI / 4.0)
      val ap = a.interpolatedPoint(t)
      return (ap, b.getNearestPoint(ap))
    }

    getNearestPoints(a, b,
      t0 = MathUtil.interpolate(t0, t1, Math.max(minI - 1, 0) / 4.0),
      t1 = MathUtil.interpolate(t0, t1, Math.min(minI + 1, 4) / 4.0),
    )
  }
