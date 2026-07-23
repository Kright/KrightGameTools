package me.kright.gametools.pga2d.geom

import me.kright.gametools.mathutil.MathUtil.{maxNanSafe, minNanSafe}
import me.kright.gametools.pga2d.{Pga2dPoint, Pga2dVector}

/**
 * special class for efficient searching of intersections with multiple AABBs
 */
final case class Pga2dRay(origin: Pga2dPoint,
                          direction: Pga2dVector,
                          directionReciprocal: Pga2dVector) {

  def hasIntersection(aabb: Pga2dAABB): Boolean =
    intersectionT(aabb) < Double.PositiveInfinity

  /**
   * @return t of the point where the ray enters the aabb (see constructors in [[Pga2dRay$]]
   *         for the meaning of t), 0.0 if origin is inside the aabb,
   *         Double.PositiveInfinity if there is no intersection
   */
  def intersectionT(aabb: Pga2dAABB): Double = {
    // intersection for 4 lines
    val t0 = (aabb.min - origin).scale(directionReciprocal)
    val t1 = (aabb.max - origin).scale(directionReciprocal)
    // near and far intersections for each axis.
    // If the ray is parallel to an axis and origin lies exactly on a slab boundary,
    // t = 0.0 * Infinity = NaN, and math.min/max propagate it: tmin and tmax get NaN
    // on that axis together. Such axis must not constrain the interval, so the
    // reductions below skip NaN components (a grazing ray counts as a hit
    // instead of collapsing the whole result to a miss).
    val tmin = t0.min(t1)
    val tmax = t0.max(t1)

    val tNear = maxNanSafe(tmin.x, tmin.y)
    val tFar = minNanSafe(tmax.x, tmax.y)

    if (tNear <= tFar && tFar > 0.0) math.max(tNear, 0.0)
    else Double.PositiveInfinity
  }
}

object Pga2dRay {
  /**
   * direction is used as is: a point on the ray is origin + direction * t,
   * so t is measured in units of direction length. t is the euclidean distance
   * to the point only if direction is normalized (see [[normalized]]).
   * Within a single ray t values are still consistent and comparable to each other.
   */
  def apply(origin: Pga2dPoint, direction: Pga2dVector): Pga2dRay = {
    Pga2dRay(origin, direction, direction.reciprocal)
  }

  /**
   * normalizes direction, so t (for example, from [[Pga2dRay.intersectionT]])
   * is the euclidean distance from origin to the point on the ray
   */
  def normalized(origin: Pga2dPoint, direction: Pga2dVector): Pga2dRay = {
    Pga2dRay(origin, direction.normalizedByNorm)
  }
}
