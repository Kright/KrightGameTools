package me.kright.gametools.pga3d.physics

import me.kright.gametools.mathutil.FastRange
import me.kright.gametools.pga3d.{Pga3dBivector, Pga3dTransform}

/**
 * Runge-Kutta-Munthe-Kaas solver with 4th order of precision.
 *
 * The problem it addresses: the motor kinematics dM/dt = M * omega (omega = -0.5 * localB,
 * the body-frame convention of [[Pga3dBodyState.derivative]]) lives on the curved manifold of
 * unit motors. [[Pga3dPhysicsSolverRK4]] treats it as a flat R^8 ODE: its intermediate stages
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
 * motor product. What it does not buy: measured on free precession, the accuracy gain over
 * renormalized RK4 is a fraction of a percent at all step sizes - the shared tableau
 * truncation error dominates, see the measurements in Pga3dPhysicsSolverRKMK4Test.
 */
object Pga3dPhysicsSolverRKMK4 extends Pga3dPhysicsSolver:
  override def step[T <: Pga3dPhysicsBody](dynamicBodies: Array[T],
                                           dt: Double,
                                           addForquesToBodies: Double => Unit): Unit = {
    val n = dynamicBodies.length
    val initial = dynamicBodies.map(Pga3dBodyState(_))

    // stage 1: u1 = 0, dexpInv is the identity
    val a1 = getAccelerations(dynamicBodies, 0.0, addForquesToBodies)
    val ku1 = initial.map(_.localB * -0.5)

    val u2 = ku1.map(_ * (0.5 * dt))
    setStage(dynamicBodies, initial, u2, a1, 0.5 * dt)
    val a2 = getAccelerations(dynamicBodies, 0.5 * dt, addForquesToBodies)
    val ku2 = Array.tabulate(n)(i => (-u2(i)).dexpInv(dynamicBodies(i).localB * -0.5))

    val u3 = ku2.map(_ * (0.5 * dt))
    setStage(dynamicBodies, initial, u3, a2, 0.5 * dt)
    val a3 = getAccelerations(dynamicBodies, 0.5 * dt, addForquesToBodies)
    val ku3 = Array.tabulate(n)(i => (-u3(i)).dexpInv(dynamicBodies(i).localB * -0.5))

    val u4 = ku3.map(_ * dt)
    setStage(dynamicBodies, initial, u4, a3, dt)
    val a4 = getAccelerations(dynamicBodies, dt, addForquesToBodies)
    val ku4 = Array.tabulate(n)(i => (-u4(i)).dexpInv(dynamicBodies(i).localB * -0.5))

    for (pos <- FastRange(n)) {
      val body = dynamicBodies(pos)
      val i = initial(pos)
      val u = (ku1(pos) + (ku2(pos) + ku3(pos)) * 2.0 + ku4(pos)) * (dt / 6.0)

      body.transform = Pga3dTransform(i.motor.geometric(u.exp))
      body.localB = i.localB + (a1(pos) + (a2(pos) + a3(pos)) * 2.0 + a4(pos)) * (dt / 6.0)
    }
  }

  private def getAccelerations(dynamicBodies: Array[? <: Pga3dPhysicsBody],
                               currentDt: Double,
                               addForquesToBodies: Double => Unit): Array[Pga3dBivector] =
    for (body <- dynamicBodies) {
      body.resetForqueAccum()
    }
    addForquesToBodies(currentDt)
    dynamicBodies.map(b => b.inertia.getAcceleration(b.localB, b.localForque))

  private def setStage(dynamicBodies: Array[? <: Pga3dPhysicsBody],
                       initial: Array[Pga3dBodyState],
                       u: Array[Pga3dBivector],
                       acceleration: Array[Pga3dBivector],
                       stageDt: Double): Unit =
    for (pos <- FastRange(dynamicBodies.length)) {
      val body = dynamicBodies(pos)
      val i = initial(pos)

      body.transform = Pga3dTransform(i.motor.geometric(u(pos).exp))
      body.localB = i.localB + acceleration(pos) * stageDt
    }

  override def toString: String =
    "Pga3dPhysicsSolverRKMK4"
