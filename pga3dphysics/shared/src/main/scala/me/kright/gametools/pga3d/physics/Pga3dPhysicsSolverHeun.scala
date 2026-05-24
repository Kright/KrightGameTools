package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.physics.Pga3dPhysicsSolverUtil.*

/** Second order of precision */
object Pga3dPhysicsSolverHeun extends Pga3dPhysicsSolver[Pga3dPhysicsBody]:
  override def step(dynamicBodies: Array[Pga3dPhysicsBody],
                    dt: Double,
                    addForquesToBodies: (Double) => Unit): Unit = {

    val initial = dynamicBodies.map(Pga3dBodyState(_))
    val k1 = getDerivative(dynamicBodies, 0.0, addForquesToBodies)

    setNewState(dynamicBodies, initial, dt, k1)
    val k2 = getDerivative(dynamicBodies, dt, addForquesToBodies)

    setNewState(dynamicBodies, initial, dt, k1, 0.5, k2, 0.5)
  }

  override def toString: String =
    "Pga3dPhysicsSolverHeun"
