package me.kright.gametools.physics1d

/**
 * Friction with memory (hysteresis) along a scalar coordinate x (e.g. a rod length):
 * unlike a stateless viscous friction, the force depends on the motion history,
 * not on the instantaneous velocity.
 *
 * The state advances once per COMPLETED integrator step via [[advance]] with the realized
 * coordinate; the very first advance only latches the starting coordinate. Mutating the
 * state inside trial RK stages is forbidden: the memory would walk a broken line several
 * times per step and catch phantom direction reversals.
 *
 * The integrator stages read the force through the PURE [[forceAt]](x) at the stage's
 * trial coordinate. Branches recomputed this way continue each other exactly across steps
 * while the motion stays monotone, so the integrator keeps its full order there (a
 * reversal inside a step is approximated by its net displacement). The price: the tangent
 * stiffness enters the integrator stability budget — but it does so through
 * [[tangentStiffness]] anyway. Evaluating forceAt at the step-start coordinate instead
 * gives the simpler frozen-force splitting (first order, needs a viscous partner).
 *
 * Sign convention matches the viscous friction convention of gametools
 * (Pga3dFriction: Linear(k)(v) = -k*v): negative force acts to decrease x, positive to
 * increase x. The friction resists the realized motion, so after the coordinate grew
 * the force is negative, and vice versa.
 */
trait HystereticFriction:
  /** the saturation force: |forceAt(x)| never exceeds it */
  def maxForce: Double

  /** pure evaluation for integrator stages: the force after a monotone move from the last
   * committed coordinate to x; does not change the state. At the committed coordinate
   * returns the committed force; before the first advance returns the initial force */
  def forceAt(x: Double): Double

  /** commits one full completed step: advances the memory along the move from the last
   * committed coordinate to x and latches x. The first call only latches the coordinate */
  def advance(x: Double): Unit

  /** the tangent stiffness of the stuck state; adds to the spring stiffness
   * in the omega*dt stability budget of the integrator */
  def tangentStiffness: Double

  /** an independent copy of the state, for deterministic physics snapshots */
  def deepCopy(): HystereticFriction
