package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.{Pga3dPoint, Pga3dVector}

/**
 * Friction with memory (hysteresis) of a point moving in a volume, the 3d counterpart of
 * [[me.kright.gametools.physics1d.HystereticFriction]]: the force on the point depends on the
 * motion history, not on the instantaneous velocity. The coordinate is a point in any frame
 * the caller chooses (typically the local frame of the body holding the bushing); the force
 * resists the realized motion, so after a move along u the force points against u.
 *
 * The same integrator contract as in 1d: the integrator stages read the force through the
 * PURE [[forceAt]] at the stage's trial point, [[advance]] commits one COMPLETED step with the
 * realized point (the first call only latches the starting point). Between commits the path is
 * replaced by its chord: exact on straight segments, and on a curved path an error of second
 * order in the step while the step travel is below the model's saturation travel, first order
 * far beyond it. Commit after every integrator step.
 *
 * Reading the force frozen over the step (forceAt at the step-start point) MUST be paired
 * with a viscous term, exactly as in 1d; reading it at the stage point removes the lag.
 */
trait Pga3dHystereticFriction:
  /** the saturation force: |forceAt| never exceeds it */
  def maxForce: Double

  /** pure evaluation for integrator stages: the force after a straight move from the last
   * committed point to (x, y, z); does not change the state. At the committed point returns
   * the committed force; before the first advance returns the initial force */
  def forceAt(x: Double, y: Double, z: Double): Pga3dVector

  /** commits one full completed step: advances the memory along the straight move from the
   * last committed point to (x, y, z) and latches it. The first call only latches the point */
  def advance(x: Double, y: Double, z: Double): Unit

  inline def forceAt(point: Pga3dPoint): Pga3dVector =
    forceAt(point.x, point.y, point.z)

  inline def advance(point: Pga3dPoint): Unit =
    advance(point.x, point.y, point.z)

  /** the tangent stiffness of the stuck state, the same in every direction; adds to the
   * spring stiffness in the omega*dt stability budget of the integrator */
  def tangentStiffness: Double

  /** an independent copy of the state, for deterministic physics snapshots */
  def deepCopy(): Pga3dHystereticFriction
