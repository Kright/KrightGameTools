package me.kright.gametools.pga3d.physics

import me.kright.gametools.physics1d.{IntegrableUniformArray, OdeSolverRK4}

/**
 * Runge-Kutta solver with 4th order of precision
 */
@deprecated("use OdeSolverRK4 of physics1d over IntegrableUniformArray(bodies, Pga3dBodyState.componentsCount)", "0.13.0")
object Pga3dPhysicsSolverRK4 extends Pga3dPhysicsSolver:
  override def step[T <: Pga3dPhysicsBody](dynamicBodies: Array[T],
                                           dt: Double,
                                           addForquesToBodies: Double => Unit): Unit =
    OdeSolverRK4.step(IntegrableUniformArray(dynamicBodies, Pga3dBodyState.componentsCount), dt, addForquesToBodies)

  override def toString: String =
    "Pga3dPhysicsSolverRK4"
