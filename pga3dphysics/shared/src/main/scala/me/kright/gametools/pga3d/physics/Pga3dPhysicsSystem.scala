package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.Pga3dBivector
import me.kright.gametools.physics1d.{IntegrableUniformArray, OdeConstraintResolver, OdeSolver, OdeSolverCache}

class Pga3dPhysicsSystem(val state: Array[Pga3dPhysicsBody],
                         val solver: OdeSolver,
                         var time: Double = 0.0,
                         val resolver: OdeConstraintResolver | Null = null):

  private val integrable = IntegrableUniformArray(state, Pga3dBodyState.componentsCount)
  private val cache = OdeSolverCache()

  def getL(): Pga3dBivector =
    Pga3dPhysicsBody.getL(state)

  def getKineticEnergy(): Double =
    Pga3dPhysicsBody.getKineticEnergy(state)

  def doStep(dt: Double, addForquesToBodies: (timeInsideStep: Double) => Unit): Unit =
    solver.step(integrable, dt, addForquesToBodies, cache, resolver)
    time += dt

  def withStateDeepCopy: Pga3dPhysicsSystem =
    require(resolver == null, "the resolver refers to the bodies of this system, a copy of the bodies needs its own one")
    Pga3dPhysicsSystem(state.map(_.deepCopy), solver, time)
