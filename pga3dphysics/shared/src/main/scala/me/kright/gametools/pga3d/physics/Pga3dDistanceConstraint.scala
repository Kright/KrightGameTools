package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.Pga3dPoint

/**
 * a hard constraint on the distance between two anchor points: the distance is kept inside
 * [minDistance, maxDistance]. Equal bounds give a rigid rod, `rope` bounds only the maximum
 * (pulls when taut, slack otherwise), `strut` bounds only the minimum (pushes when compressed).
 *
 * Bodies are referenced by their index in the `dynamicBodies` array passed to the solver;
 * [[Pga3dDistanceConstraint.world]] anchors the point to the static world (then the anchor is
 * in world coordinates, otherwise in body-local coordinates).
 *
 * Resolved by [[Pga3dConstraintResolver]]; see [[Pga3dPhysicsSolverConstrained]] for plugging
 * it into a solver.
 */
final case class Pga3dDistanceConstraint(bodyA: Int,
                                         anchorA: Pga3dPoint,
                                         bodyB: Int,
                                         anchorB: Pga3dPoint,
                                         minDistance: Double,
                                         maxDistance: Double):

  /**
   * one-sided activation: a rope (at the max bound) may only pull the anchors together
   * (lambda <= 0), a strut (at the min bound) may only push them apart (lambda >= 0), a rod
   * (equal bounds) does both regardless of which side of the bound the current distance
   * drifted to; strictly inside the bounds the constraint is inactive
   */
  def clampLambda(dist: Double, lambda: Double): Double =
    if (minDistance >= maxDistance) return lambda

    val slop = 1e-9 * (1.0 + dist)
    if (dist >= maxDistance - slop) Math.min(lambda, 0.0)
    else if (dist <= minDistance + slop) Math.max(lambda, 0.0)
    else 0.0

object Pga3dDistanceConstraint:
  /** pass as a body index to anchor that side to the static world */
  val world: Int = -1

  def rod(bodyA: Int, anchorA: Pga3dPoint, bodyB: Int, anchorB: Pga3dPoint,
          distance: Double): Pga3dDistanceConstraint =
    Pga3dDistanceConstraint(bodyA, anchorA, bodyB, anchorB, distance, distance)

  def rope(bodyA: Int, anchorA: Pga3dPoint, bodyB: Int, anchorB: Pga3dPoint,
           maxDistance: Double): Pga3dDistanceConstraint =
    Pga3dDistanceConstraint(bodyA, anchorA, bodyB, anchorB, 0.0, maxDistance)

  def strut(bodyA: Int, anchorA: Pga3dPoint, bodyB: Int, anchorB: Pga3dPoint,
            minDistance: Double): Pga3dDistanceConstraint =
    Pga3dDistanceConstraint(bodyA, anchorA, bodyB, anchorB, minDistance, Double.PositiveInfinity)
