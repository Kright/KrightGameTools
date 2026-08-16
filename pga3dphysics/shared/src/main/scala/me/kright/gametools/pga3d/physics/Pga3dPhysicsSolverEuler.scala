package me.kright.gametools.pga3d.physics

import me.kright.gametools.pga3d.physics.Pga3dPhysicsSolverUtil.{getDerivative, setNewState}

/** First order of precision. Very imprecise */
object Pga3dPhysicsSolverEuler extends Pga3dPhysicsSolver:
  override def step[T <: Pga3dPhysicsBody](dynamicBodies: Array[T],
                                           dt: Double,
                                           addForquesToBodies: (Double) => Unit): Unit = {

    val initial = dynamicBodies.map(Pga3dBodyState(_))
    val k1 = getDerivative(dynamicBodies, 0.0, addForquesToBodies)
    setNewState(dynamicBodies, initial, dt, k1)
  }

  override def toString: String =
    "Pga3dPhysicsSolverEuler"
