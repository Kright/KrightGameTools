package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.physics.Pga3dPhysicsSolverUtil.*

/**
 * Runge-Kutta solver with 4th order of precision
 */
object Pga3dPhysicsSolverRK4 extends Pga3dPhysicsSolver[Pga3dPhysicsBody]:
  override def step(dynamicBodies: Array[Pga3dPhysicsBody],
                    dt: Double,
                    addForquesToBodies: (Double) => Unit): Unit = {

    val initial = dynamicBodies.map(Pga3dBodyState(_))

    val k1 = getDerivative(dynamicBodies, 0.0, addForquesToBodies)

    setNewState(dynamicBodies, initial, 0.5 * dt, k1)
    val k2 = getDerivative(dynamicBodies, 0.5 * dt, addForquesToBodies)

    setNewState(dynamicBodies, initial, 0.5 * dt, k2)
    val k3 = getDerivative(dynamicBodies, 0.5 * dt, addForquesToBodies)

    setNewState(dynamicBodies, initial, dt, k3)
    val k4 = getDerivative(dynamicBodies, dt, addForquesToBodies)

    setNewState(dynamicBodies, initial, dt, k1, 1.0 / 6.0, k2, 2.0 / 6.0, k3, 2.0 / 6.0, k4, 1.0 / 6.0)
  }

  override def toString: String =
    "Pga3dPhysicsSolverRK4"
