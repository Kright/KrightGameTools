package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.Pga3dBivector
import me.kright.gametools.physics1d.{IntegrableUniformArray, OdeConstraintResolver, OdeSolver, OdeSolverCache}

class Pga3dPhysicsSystem[T <: Pga3dPhysicsBody](val bodies: Array[T],
                                                val solver: OdeSolver,
                                                var time: Double = 0.0,
                                                val resolver: OdeConstraintResolver | Null = null):

  private val integrable = IntegrableUniformArray(bodies, Pga3dBodyState.componentsCount)
  private val cache = OdeSolverCache()

  def getL(): Pga3dBivector =
    Pga3dPhysicsBody.getL(bodies)

  def getKineticEnergy(): Double =
    Pga3dPhysicsBody.getKineticEnergy(bodies)

  def doStep(dt: Double, addForces: (timeInsideStep: Double) => Unit): Unit =
    solver.step(integrable, dt, addForces, cache, resolver)
    time += dt
