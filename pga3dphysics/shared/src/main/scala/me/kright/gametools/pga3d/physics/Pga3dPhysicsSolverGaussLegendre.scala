package me.kright.gametools.pga3d.physics

import me.kright.gametools.physics1d.{IntegrableUniformArray, OdeSolverGaussLegendre}

/**
 * [[https://en.wikipedia.org/wiki/Gauss%E2%80%93Legendre_method]]
 *
 * @param iterations it looks like optimal iterations count is 3 or more. 
 *                   With two iterations precision is similar with [[Pga3dPhysicsSolverRK4]], but RK4 is faster to compute
 */
@deprecated("use OdeSolverGaussLegendre of physics1d over IntegrableUniformArray(bodies, Pga3dBodyState.componentsCount)", "0.13.0")
class Pga3dPhysicsSolverGaussLegendre(val iterations: Int = 3) extends Pga3dPhysicsSolver:
  private val solver = OdeSolverGaussLegendre(iterations)

  override def step[T <: Pga3dPhysicsBody](dynamicBodies: Array[T], dt: Double, addForquesToBodies: Double => Unit): Unit =
    solver.step(IntegrableUniformArray(dynamicBodies, Pga3dBodyState.componentsCount), dt, addForquesToBodies)

  override def toString: String =
    s"Pga3dPhysicsSolverGaussLegendre(iterations=$iterations)"
