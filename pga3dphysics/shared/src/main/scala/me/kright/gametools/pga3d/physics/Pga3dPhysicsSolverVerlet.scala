package me.kright.gametools.pga3d.physics

import me.kright.gametools.mathutil.FastRange
import me.kright.gametools.pga3d.{Pga3dBivector, Pga3dMotor}

/**
 * Position-Verlet (leapfrog) on the motor group, 2nd order of precision, one force evaluation
 * per step.
 *
 * This is a stateless computation strategy, not a [[Pga3dPhysicsSolver]]: for Verlet the state
 * is two consecutive pose arrays owned by the caller, and the velocity is not stored anywhere -
 * it is derived on the fly as the Lie derivative of the pose,
 * `B = -2 * (prevMotor.reverse * motor).log / prevDt`, the group analog of "velocity is the
 * difference of two positions". The kick is applied to the world-frame momentum (the discrete
 * Moser-Veselov idea): the momentum of the last segment is derived from the poses, the forque
 * impulse is added in the world frame, and the next displacement is solved from the momentum
 * by a midframe fixed-point - the gyroscopic term never appears explicitly and the momentum of
 * a free body is transported exactly.
 *
 * Usage sketch (the caller owns and rotates the pose arrays):
 * {{{
 * var prev = Pga3dPhysicsSolverVerlet.makePrevMotors(inertias, motors, localBs, forques, dt)
 * while (running) {
 *   val forques = computeForques(motors, localBs) // once per step, at the current poses
 *   Pga3dPhysicsSolverVerlet.step(inertias, prev, motors, forques, dt, dt, next, localBs)
 *   val tmp = prev; prev = motors; motors = next; next = tmp
 * }
 * }}}
 * `nextMotors` may alias `prevMotors` (each previous pose is consumed before the output is
 * written), which gives the classic two-array rotation. `nextLocalBs` receives the node-time
 * twist derived from the new momentum - use it for the next force evaluation, kinetic energy
 * and momentum queries.
 *
 * Because the poses are the only state, editing a motor between steps (position-based
 * depenetration) implicitly edits the velocity, exactly like in position-based dynamics; a
 * teleport must rewrite both pose arrays, otherwise the jump is read as a huge velocity.
 * The rotation per step must stay below pi (the motor log wraps).
 */
object Pga3dPhysicsSolverVerlet:

  /** the half-step twist of the segment [prevMotor, motor]: the Lie derivative of the pose */
  def segmentLocalB(prevMotor: Pga3dMotor, motor: Pga3dMotor, prevDt: Double): Pga3dBivector =
    prevMotor.reverse.geometric(motor).log * (-2.0 / prevDt)

  /**
   * one Verlet step: derive the segment momenta from (prevMotors, motors), kick them by the
   * world forques over (prevDt + dt) / 2, and drift into nextMotors
   *
   * @param globalForques world forques evaluated at the current poses
   * @param nextMotors    output; may alias prevMotors
   * @param nextLocalBs   output: the node-time twists at the new poses
   */
  def step(inertias: Array[Pga3dInertia],
           prevMotors: Array[Pga3dMotor],
           motors: Array[Pga3dMotor],
           globalForques: Array[Pga3dBivector],
           prevDt: Double,
           dt: Double,
           nextMotors: Array[Pga3dMotor],
           nextLocalBs: Array[Pga3dBivector]): Unit =
    val kickDt = 0.5 * (prevDt + dt)
    for (pos <- FastRange(motors.length)) {
      val inertia = inertias(pos)
      val motor = motors(pos)
      val prevMotor = prevMotors(pos)

      val d = prevMotor.reverse.geometric(motor).log
      val bHalf = d * (-2.0 / prevDt)
      val midFrame = prevMotor.geometric((d * 0.5).exp)
      val l = midFrame.sandwich(inertia(bHalf)) + globalForques(pos) * kickDt

      val displacement = solveDisplacement(inertia, motor, bHalf, l, dt)
      val next = motor.geometric(displacement.exp).renormalized
      nextMotors(pos) = next
      nextLocalBs(pos) = inertia.invert(next.reverseSandwich(l))
    }

  /**
   * virtual previous poses for the first step, built from the node velocities localBs and a
   * backward half kick by the world forques at the initial poses - the standard 2nd-order
   * leapfrog start
   */
  def makePrevMotors(inertias: Array[Pga3dInertia],
                     motors: Array[Pga3dMotor],
                     localBs: Array[Pga3dBivector],
                     globalForques: Array[Pga3dBivector],
                     dt: Double): Array[Pga3dMotor] =
    Array.tabulate(motors.length) { pos =>
      val inertia = inertias(pos)
      val motor = motors(pos)
      val l = motor.sandwich(inertia(localBs(pos))) - globalForques(pos) * (0.5 * dt)

      // the previous pose is one drift backward in time from the same momentum
      val d = solveDisplacement(inertia, motor, localBs(pos), l, -dt)
      motor.geometric(d.exp).renormalized
    }

  private val maxFixedPointIterations = 32

  /**
   * solves `(motor * exp(d/2)).sandwich(inertia(-2d/dt)) = lWorld` for the displacement d
   * (a negative dt drifts backward in time - see [[makePrevMotors]]); iterated to convergence
   * so the momentum implied by the poses stays exact - a fixed residual here would show up as
   * a slow energy drift
   */
  private[physics] def solveDisplacement(inertia: Pga3dInertia,
                                         motor: Pga3dMotor,
                                         guess: Pga3dBivector,
                                         lWorld: Pga3dBivector,
                                         dt: Double): Pga3dBivector =
    var d = guess * (-0.5 * dt)
    for (_ <- FastRange(maxFixedPointIterations)) {
      val midFrame = motor.geometric((d * 0.5).exp)
      val next = inertia.invert(midFrame.reverseSandwich(lWorld)) * (-0.5 * dt)
      val delta = (next - d).norm
      d = next
      if (delta <= 1e-15 * (1.0 + d.norm)) return d
    }
    d
