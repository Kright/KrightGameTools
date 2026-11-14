package me.kright.gametools.pga3d.physics

import me.kright.gametools.mathutil.FastRange
import me.kright.gametools.pga3d.{Pga3dBivector, Pga3dMotor, Pga3dTransform}
import me.kright.gametools.physics1d.{Integrable, OdeConstraintResolver, OdeSolver, OdeSolverCache}
import me.kright.gametools.physics1d.OdeSolverUtil.{combine, saveDerivative}

/**
 * Runge-Kutta-Munthe-Kaas solver with 4th order of precision.
 *
 * The problem it addresses: the motor kinematics dM/dt = M * omega (omega = -0.5 * localB,
 * the body-frame convention of [[Pga3dBodyState.derivative]]) lives on the curved manifold of
 * unit motors. `OdeSolverRK4` of physics1d treats it as a flat R^8 ODE: its intermediate stages
 * M0 + (h/2)k are not motors at all and are projected back onto the group by renormalization
 * ([[Pga3dMotor.renormalized]] is a genuine manifold projection, and the measured order stays 4).
 * RKMK4 never leaves the group in the first place: the change of variables through exp and
 * dexpInv keeps every stage a motor by construction.
 *
 * The Munthe-Kaas idea is a change of variables before the same Runge-Kutta method: within a
 * step, look for a bivector u(t) such that M(t) = M0 * exp(u(t)). Bivectors form the flat Lie
 * algebra of the motor group (they are closed under the commutator), so u can be integrated by
 * the classic RK4 tableau (stages 0, 1/2, 1/2, 1 with weights 1/6, 2/6, 2/6, 1/6 - the tableau
 * is unchanged, only the space is different), and the result comes back through a single exp.
 *
 * The subtlety is that for non-commuting objects d/dt exp(u) != u' * exp(u), so u does not
 * satisfy u' = omega. The correction factor is the differential of exp: for the right-
 * trivialized form used here (M = M0 * exp(u), body-frame twist) it works out to
 *
 *   du/dt = dexpInv(-u, omega) = (-u).dexpInv(omega)
 *
 * via the closed-form [[Pga3dBivector.dexpInv]] (the library implements the left-trivialized
 * variant; the right one is its mirror u -> -u, see the pga-concepts.md notes on dexp).
 * A Bernoulli-series truncation omega + [u, omega]/2 + [u, [u, omega]]/12 would already
 * preserve the 4th order (within a step u = O(h) and the first dropped term is O(h^5)), but
 * the exact form costs about the same and keeps the series bookkeeping out of the solver.
 *
 * What this buys compared to RK4: every stage motor M0 * exp(u_i) and the result are exact
 * motors by construction, the right-hand side is always evaluated at a legal point of the
 * configuration space, and the final renormalization only removes the rounding noise of the
 * motor product. A step with a twist constant in the body frame (a spherical inertia, a spin
 * about a principal axis) is exact, where RK4 keeps the error of its rotation per step. For a
 * tumbling asymmetric body the gain over renormalized RK4 is a fraction of a percent at all
 * step sizes - the shared tableau truncation error dominates. The measurements are in
 * Solvers.md and Pga3dPhysicsSolverRKMK4Test.
 *
 * As an [[OdeSolver]] it integrates the whole system by the classic RK4 over the flat state and
 * replaces only the motors of [[dynamicBodies]] by the group update, so the bodies must be a
 * part of the system given to [[step]]. Everything else of the system, localB of the bodies
 * included, goes through the plain RK4 stages.
 */
class Pga3dPhysicsSolverRKMK4(val dynamicBodies: Array[? <: Pga3dPhysicsBody]) extends OdeSolver:
  override def step(system: Integrable,
                    dt: Double,
                    addForces: (timeInsideStep: Double) => Unit,
                    cache: OdeSolverCache | Null,
                    resolver: OdeConstraintResolver | Null): Unit = {
    val size = system.stateSize
    val arrays = OdeSolverCache.getArrays(cache, 6, size)
    val initial = arrays(0)
    val k1 = arrays(1)
    val k2 = arrays(2)
    val k3 = arrays(3)
    val k4 = arrays(4)
    val stage = arrays(5)

    val initialMotors = dynamicBodies.map(_.motor)

    // stage 1: u1 = 0, dexpInv is the identity
    val ku1 = dynamicBodies.map(_.localB * -0.5)
    system.saveState(initial, 0)
    saveDerivative(system, 0.0, addForces, resolver, k1)

    val u2 = ku1.map(_ * (0.5 * dt))
    combine(stage, size, initial, 0.5 * dt, k1)
    system.loadState(stage, 0)
    setMotors(initialMotors, u2)
    saveDerivative(system, 0.5 * dt, addForces, resolver, k2)
    val ku2 = getAlgebraVelocities(u2)

    val u3 = ku2.map(_ * (0.5 * dt))
    combine(stage, size, initial, 0.5 * dt, k2)
    system.loadState(stage, 0)
    setMotors(initialMotors, u3)
    saveDerivative(system, 0.5 * dt, addForces, resolver, k3)
    val ku3 = getAlgebraVelocities(u3)

    val u4 = ku3.map(_ * dt)
    combine(stage, size, initial, dt, k3)
    system.loadState(stage, 0)
    setMotors(initialMotors, u4)
    saveDerivative(system, dt, addForces, resolver, k4)
    val ku4 = getAlgebraVelocities(u4)

    val u = Array.tabulate(dynamicBodies.length) { pos =>
      (ku1(pos) + (ku2(pos) + ku3(pos)) * 2.0 + ku4(pos)) * (dt / 6.0)
    }
    combine(stage, size, initial, dt, k1, 1.0 / 6.0, k2, 2.0 / 6.0, k3, 2.0 / 6.0, k4, 1.0 / 6.0)
    system.loadState(stage, 0)
    setMotors(initialMotors, u)
    if (resolver != null) {
      resolver.project()
    }
  }

  private def setMotors(initialMotors: Array[Pga3dMotor], u: Array[Pga3dBivector]): Unit =
    for (pos <- FastRange(dynamicBodies.length)) {
      dynamicBodies(pos).transform = Pga3dTransform(initialMotors(pos).geometric(u(pos).exp))
    }

  /** du/dt of every body at its current localB */
  private def getAlgebraVelocities(u: Array[Pga3dBivector]): Array[Pga3dBivector] =
    Array.tabulate(dynamicBodies.length)(pos => (-u(pos)).dexpInv(dynamicBodies(pos).localB * -0.5))

  override def toString: String =
    "Pga3dPhysicsSolverRKMK4"
