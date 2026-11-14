package me.kright.gametools.pga3d.physics

import me.kright.gametools.physics1d.{IntegrableUniformArray, OdeSolverHeun}

/** Second order of precision */
@deprecated("use OdeSolverHeun of physics1d over IntegrableUniformArray(bodies, Pga3dBodyState.componentsCount)", "0.13.0")
object Pga3dPhysicsSolverHeun extends Pga3dPhysicsSolver:
  override def step[T <: Pga3dPhysicsBody](dynamicBodies: Array[T],
                                           dt: Double,
                                           addForquesToBodies: Double => Unit): Unit =
    OdeSolverHeun.step(IntegrableUniformArray(dynamicBodies, Pga3dBodyState.componentsCount), dt, addForquesToBodies)

  override def toString: String =
    "Pga3dPhysicsSolverHeun"
